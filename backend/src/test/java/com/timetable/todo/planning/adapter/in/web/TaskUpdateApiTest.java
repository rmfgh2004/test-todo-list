package com.timetable.todo.planning.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import tools.jackson.databind.JsonNode;

/**
 * FR-004, FR-007, NFR-006: Task content edit contract.
 *
 * <p>PATCH replaces the full content set: title, priority and estimateMinutes are required and an
 * omitted description or dueDate clears that value. Status and schedule placement are owned by
 * their own endpoints. When a new estimate resizes an existing placement, the start time is kept
 * and the end time is recalculated under the same FR-007 conflict rules.
 */
class TaskUpdateApiTest extends AbstractApiContractTest {

  private static final String MONDAY = "2026-09-07";

  private void schedule(String id, String startTime, long expectedVersion) throws Exception {
    mvc.perform(
            put(TASKS + "/" + id + "/schedule")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"date\":\"%s\",\"startTime\":\"%s\",\"expectedVersion\":%d}"
                        .formatted(MONDAY, startTime, expectedVersion)))
        .andExpect(status().isOk());
  }

  @Test
  void FR_004_replaces_the_full_content_set() throws Exception {
    JsonNode task = createTask("Original", 30, "LOW", LocalDate.parse("2026-09-09"));

    mvc.perform(
            patch(TASKS + "/" + task.get("id").asText())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"expectedVersion":0,"title":"Rewritten","description":"new detail",\
                    "priority":"HIGH","estimateMinutes":45,"dueDate":"2026-09-11"}"""))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.title").value("Rewritten"))
        .andExpect(jsonPath("$.description").value("new detail"))
        .andExpect(jsonPath("$.priority").value("HIGH"))
        .andExpect(jsonPath("$.estimateMinutes").value(45))
        .andExpect(jsonPath("$.dueDate").value("2026-09-11"))
        .andExpect(jsonPath("$.status").value("TODO"))
        .andExpect(jsonPath("$.version").value(1));

    assertThat(store.auditRecords()).contains("UPDATED:content");
  }

  @Test
  void FR_004_omitted_optional_fields_are_cleared() throws Exception {
    JsonNode task = createTask("Has extras", 30, "LOW", LocalDate.parse("2026-09-09"));

    mvc.perform(
            patch(TASKS + "/" + task.get("id").asText())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"expectedVersion":0,"title":"Has extras","priority":"LOW",\
                    "estimateMinutes":30}"""))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.description").doesNotExist())
        .andExpect(jsonPath("$.dueDate").doesNotExist());
  }

  @Test
  void FR_004_update_does_not_change_completion_state() throws Exception {
    JsonNode task = createTask("Completed content", 30);
    String id = task.get("id").asText();
    mvc.perform(
            put(TASKS + "/" + id + "/completion")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"completed\":true,\"expectedVersion\":0}"))
        .andExpect(status().isOk());

    mvc.perform(
            patch(TASKS + "/" + id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"expectedVersion":1,"title":"Renamed","priority":"MEDIUM",\
                    "estimateMinutes":30}"""))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("COMPLETED"))
        .andExpect(jsonPath("$.title").value("Renamed"));
  }

  @Test
  void FR_004_applies_the_same_validation_as_create() throws Exception {
    JsonNode task = createTask("Validated", 30);

    mvc.perform(
            patch(TASKS + "/" + task.get("id").asText())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"expectedVersion":0,"title":"   ","priority":"MEDIUM",\
                    "estimateMinutes":20}"""))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
        .andExpect(jsonPath("$.fieldErrors.length()").value(2));
  }

  @Test
  void FR_004_requires_expected_version() throws Exception {
    JsonNode task = createTask("Needs version", 30);

    mvc.perform(
            patch(TASKS + "/" + task.get("id").asText())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"No version\",\"priority\":\"LOW\",\"estimateMinutes\":30}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
        .andExpect(jsonPath("$.fieldErrors[0].field").value("expectedVersion"));
  }

  @Test
  void NFR_006_rejects_a_stale_update_without_mutation() throws Exception {
    JsonNode task = createTask("Stale update", 30);
    String id = task.get("id").asText();

    mvc.perform(
            patch(TASKS + "/" + id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"expectedVersion":9,"title":"Should not apply","priority":"LOW",\
                    "estimateMinutes":30}"""))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("STALE_TASK"))
        .andExpect(jsonPath("$.currentVersion").value(0));

    mvc.perform(get(TASKS + "/" + id)).andExpect(jsonPath("$.title").value("Stale update"));
  }

  @Test
  void FR_004_returns_safe_not_found_for_an_unknown_task() throws Exception {
    mvc.perform(
            patch(TASKS + "/" + UUID.randomUUID())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"expectedVersion":0,"title":"Ghost","priority":"LOW",\
                    "estimateMinutes":30}"""))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("TASK_NOT_FOUND"));
  }

  @Test
  void FR_007_resizes_the_existing_slot_when_the_estimate_grows() throws Exception {
    JsonNode task = createTask("Growing", 30);
    String id = task.get("id").asText();
    schedule(id, "09:00", 0);

    mvc.perform(
            patch(TASKS + "/" + id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"expectedVersion":1,"title":"Growing","priority":"MEDIUM",\
                    "estimateMinutes":60}"""))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.estimateMinutes").value(60))
        .andExpect(jsonPath("$.schedule.startTime").value("09:00"))
        .andExpect(jsonPath("$.schedule.endTime").value("10:00"))
        .andExpect(jsonPath("$.version").value(2));
  }

  @Test
  void FR_007_returns_conflict_when_the_resized_slot_overlaps() throws Exception {
    JsonNode growing = createTask("Growing", 30);
    JsonNode neighbour = createTask("Neighbour", 60);
    String growingId = growing.get("id").asText();
    schedule(growingId, "09:00", 0);
    schedule(neighbour.get("id").asText(), "09:30", 0);

    String payload =
        mvc.perform(
                patch(TASKS + "/" + growingId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {"expectedVersion":1,"title":"Growing","priority":"MEDIUM",\
                        "estimateMinutes":60}"""))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("SCHEDULE_CONFLICT"))
            .andExpect(jsonPath("$.conflict.proposed.startTime").value("09:00"))
            .andExpect(jsonPath("$.conflict.proposed.endTime").value("10:00"))
            .andExpect(jsonPath("$.conflict.conflicting.startTime").value("09:30"))
            .andExpect(jsonPath("$.conflict.nextCandidate.startTime").value("10:30"))
            .andReturn()
            .getResponse()
            .getContentAsString();

    assertSafeErrorPayload(payload);

    mvc.perform(get(TASKS + "/" + growingId))
        .andExpect(jsonPath("$.estimateMinutes").value(30))
        .andExpect(jsonPath("$.schedule.endTime").value("09:30"))
        .andExpect(jsonPath("$.version").value(1));
  }

  @Test
  void FR_006_rejects_a_resize_that_leaves_the_planning_window() throws Exception {
    JsonNode task = createTask("Late block", 30);
    String id = task.get("id").asText();
    schedule(id, "21:30", 0);

    mvc.perform(
            patch(TASKS + "/" + id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"expectedVersion":1,"title":"Late block","priority":"MEDIUM",\
                    "estimateMinutes":60}"""))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("SCHEDULE_OUT_OF_WINDOW"));

    mvc.perform(get(TASKS + "/" + id)).andExpect(jsonPath("$.estimateMinutes").value(30));
  }

  @Test
  void FR_004_keeps_the_placement_when_the_estimate_is_unchanged() throws Exception {
    JsonNode task = createTask("Stable", 30);
    String id = task.get("id").asText();
    schedule(id, "09:00", 0);

    mvc.perform(
            patch(TASKS + "/" + id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"expectedVersion":1,"title":"Stable renamed","priority":"HIGH",\
                    "estimateMinutes":30}"""))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.schedule.startTime").value("09:00"))
        .andExpect(jsonPath("$.schedule.endTime").value("09:30"));
  }
}
