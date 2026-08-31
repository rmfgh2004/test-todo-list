package com.timetable.todo.planning.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import tools.jackson.databind.JsonNode;

/** FR-001, FR-002, FR-005, FR-010, NFR-005: Bounded planning query contract over HTTP. */
class PlanningQueryApiTest extends AbstractApiContractTest {

  private void schedule(String id, String date, String startTime, long expectedVersion)
      throws Exception {
    mvc.perform(
            put(TASKS + "/" + id + "/schedule")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"date\":\"%s\",\"startTime\":\"%s\",\"expectedVersion\":%d}"
                        .formatted(date, startTime, expectedVersion)))
        .andExpect(status().isOk());
  }

  @Test
  void FR_001_returns_bounded_week_plan_with_scheduled_and_backlog() throws Exception {
    JsonNode scheduled = createTask("Scheduled item", 30);
    createTask("Backlog item", 60, "HIGH", LocalDate.parse("2026-09-08"));
    schedule(scheduled.get("id").asText(), "2026-09-07", "09:00", 0);

    mvc.perform(get(WEEKS + "/" + WEEK_START))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.weekStart").value("2026-09-07"))
        .andExpect(jsonPath("$.weekEndExclusive").value("2026-09-14"))
        .andExpect(jsonPath("$.scheduled.length()").value(1))
        .andExpect(jsonPath("$.scheduled[0].title").value("Scheduled item"))
        .andExpect(jsonPath("$.scheduled[0].schedule.startTime").value("09:00"))
        .andExpect(jsonPath("$.backlog.length()").value(1))
        .andExpect(jsonPath("$.backlog[0].title").value("Backlog item"));
  }

  @Test
  void FR_001_week_plan_excludes_tasks_scheduled_in_another_week() throws Exception {
    JsonNode task = createTask("Next week", 30);
    schedule(task.get("id").asText(), "2026-09-14", "09:00", 0);

    mvc.perform(get(WEEKS + "/" + WEEK_START))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.scheduled.length()").value(0));
  }

  @Test
  void FR_009_week_plan_still_shows_completed_scheduled_tasks() throws Exception {
    JsonNode task = createTask("Finished but placed", 30);
    String id = task.get("id").asText();
    schedule(id, "2026-09-07", "09:00", 0);
    mvc.perform(
            put(TASKS + "/" + id + "/completion")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"completed\":true,\"expectedVersion\":1}"))
        .andExpect(status().isOk());

    mvc.perform(get(WEEKS + "/" + WEEK_START))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.scheduled.length()").value(1))
        .andExpect(jsonPath("$.scheduled[0].status").value("COMPLETED"))
        .andExpect(jsonPath("$.backlog.length()").value(0));
  }

  @Test
  void FR_001_rejects_week_start_that_is_not_monday() throws Exception {
    String payload =
        mvc.perform(get(WEEKS + "/2026-09-08"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("WEEK_START_INVALID"))
            .andExpect(jsonPath("$.requestId").isNotEmpty())
            .andReturn()
            .getResponse()
            .getContentAsString();

    assertSafeErrorPayload(payload);
  }

  @Test
  void FR_001_rejects_malformed_week_start() throws Exception {
    String payload =
        mvc.perform(get(WEEKS + "/2026-13-99"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
            .andReturn()
            .getResponse()
            .getContentAsString();

    assertThat(payload).doesNotContain("2026-13-99");
    assertSafeErrorPayload(payload);
  }

  @Test
  void FR_005_backlog_excludes_scheduled_and_completed_tasks() throws Exception {
    JsonNode placed = createTask("Placed", 30);
    JsonNode done = createTask("Done", 30);
    createTask("Still open", 30);
    schedule(placed.get("id").asText(), "2026-09-07", "11:00", 0);
    mvc.perform(
            put(TASKS + "/" + done.get("id").asText() + "/completion")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"completed\":true,\"expectedVersion\":0}"))
        .andExpect(status().isOk());

    mvc.perform(get(WEEKS + "/" + WEEK_START))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.backlog.length()").value(1))
        .andExpect(jsonPath("$.backlog[0].title").value("Still open"));
  }

  @Test
  void FR_005_backlog_orders_by_due_date_then_priority() throws Exception {
    createTask("No due low", 30, "LOW", null);
    createTask("Later due", 30, "LOW", LocalDate.parse("2026-09-12"));
    createTask("Earliest due", 30, "LOW", LocalDate.parse("2026-09-08"));
    createTask("No due high", 30, "HIGH", null);

    mvc.perform(get(WEEKS + "/" + WEEK_START))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.backlog[0].title").value("Earliest due"))
        .andExpect(jsonPath("$.backlog[1].title").value("Later due"))
        .andExpect(jsonPath("$.backlog[2].title").value("No due high"))
        .andExpect(jsonPath("$.backlog[3].title").value("No due low"));
  }

  @Test
  void FR_010_lists_tasks_with_bounded_default_page() throws Exception {
    createTask("Listed", 30);

    mvc.perform(get(TASKS))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.page").value(0))
        .andExpect(jsonPath("$.size").value(25))
        .andExpect(jsonPath("$.totalElements").value(1))
        .andExpect(jsonPath("$.totalPages").value(1))
        .andExpect(jsonPath("$.content[0].title").value("Listed"));
  }

  @Test
  void FR_010_paginates_with_stable_totals() throws Exception {
    createTask("A", 30);
    createTask("B", 30);
    createTask("C", 30);

    mvc.perform(get(TASKS).param("size", "2").param("sort", "TITLE").param("direction", "ASC"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()").value(2))
        .andExpect(jsonPath("$.content[0].title").value("A"))
        .andExpect(jsonPath("$.content[1].title").value("B"))
        .andExpect(jsonPath("$.totalElements").value(3))
        .andExpect(jsonPath("$.totalPages").value(2));

    mvc.perform(
            get(TASKS)
                .param("size", "2")
                .param("page", "1")
                .param("sort", "TITLE")
                .param("direction", "ASC"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()").value(1))
        .andExpect(jsonPath("$.content[0].title").value("C"));
  }

  @Test
  void FR_010_filters_by_status_priority_and_schedule_state() throws Exception {
    JsonNode placed = createTask("Placed high", 30, "HIGH", null);
    createTask("Open low", 30, "LOW", null);
    schedule(placed.get("id").asText(), "2026-09-07", "13:00", 0);

    mvc.perform(get(TASKS).param("scheduled", "true"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(1))
        .andExpect(jsonPath("$.content[0].title").value("Placed high"));

    mvc.perform(get(TASKS).param("scheduled", "false"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(1))
        .andExpect(jsonPath("$.content[0].title").value("Open low"));

    mvc.perform(get(TASKS).param("priority", "HIGH"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(1));

    mvc.perform(get(TASKS).param("status", "COMPLETED"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(0));
  }

  @Test
  void NFR_005_rejects_page_size_above_the_bound() throws Exception {
    mvc.perform(get(TASKS).param("size", "101"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("PAGE_SIZE_INVALID"));
  }

  @Test
  void NFR_005_rejects_non_positive_page_size() throws Exception {
    mvc.perform(get(TASKS).param("size", "0"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("PAGE_SIZE_INVALID"));
  }

  @Test
  void NFR_005_rejects_negative_page_index() throws Exception {
    mvc.perform(get(TASKS).param("page", "-1"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("PAGE_INVALID"));
  }

  @Test
  void FR_010_rejects_sort_value_outside_the_allowlist() throws Exception {
    String payload =
        mvc.perform(get(TASKS).param("sort", "id; drop table tasks"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
            .andReturn()
            .getResponse()
            .getContentAsString();

    assertThat(payload).doesNotContain("drop table");
    assertSafeErrorPayload(payload);
  }
}
