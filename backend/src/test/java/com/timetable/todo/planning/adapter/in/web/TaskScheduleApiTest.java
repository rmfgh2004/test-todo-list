package com.timetable.todo.planning.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import tools.jackson.databind.JsonNode;

/** FR-006, FR-007, FR-008, NFR-006: Scheduling contract over HTTP. */
class TaskScheduleApiTest extends AbstractApiContractTest {

  private static final String MONDAY = "2026-09-07";

  private String scheduleBody(String date, String startTime, long expectedVersion) {
    return "{\"date\":\"%s\",\"startTime\":\"%s\",\"expectedVersion\":%d}"
        .formatted(date, startTime, expectedVersion);
  }

  @Test
  void FR_006_schedules_task_and_derives_end_time() throws Exception {
    JsonNode task = createTask("Deep work", 45);

    mvc.perform(
            put(TASKS + "/" + task.get("id").asText() + "/schedule")
                .contentType(MediaType.APPLICATION_JSON)
                .content(scheduleBody(MONDAY, "09:00", 0)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.schedule.date").value(MONDAY))
        .andExpect(jsonPath("$.schedule.startTime").value("09:00"))
        .andExpect(jsonPath("$.schedule.endTime").value("09:45"))
        .andExpect(jsonPath("$.version").value(1));
  }

  @Test
  void FR_006_rejects_start_time_outside_quarter_grid() throws Exception {
    JsonNode task = createTask("Misaligned", 30);

    mvc.perform(
            put(TASKS + "/" + task.get("id").asText() + "/schedule")
                .contentType(MediaType.APPLICATION_JSON)
                .content(scheduleBody(MONDAY, "09:10", 0)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("SCHEDULE_ALIGNMENT_INVALID"))
        .andExpect(jsonPath("$.requestId").isNotEmpty());
  }

  @Test
  void FR_006_rejects_schedule_before_planning_window() throws Exception {
    JsonNode task = createTask("Too early", 30);

    mvc.perform(
            put(TASKS + "/" + task.get("id").asText() + "/schedule")
                .contentType(MediaType.APPLICATION_JSON)
                .content(scheduleBody(MONDAY, "07:45", 0)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("SCHEDULE_OUT_OF_WINDOW"));
  }

  @Test
  void FR_006_rejects_schedule_ending_after_planning_window() throws Exception {
    JsonNode task = createTask("Too late", 30);

    mvc.perform(
            put(TASKS + "/" + task.get("id").asText() + "/schedule")
                .contentType(MediaType.APPLICATION_JSON)
                .content(scheduleBody(MONDAY, "21:45", 0)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("SCHEDULE_OUT_OF_WINDOW"));
  }

  @Test
  void FR_007_touching_slots_are_not_a_conflict() throws Exception {
    JsonNode first = createTask("First", 30);
    JsonNode second = createTask("Second", 30);

    mvc.perform(
            put(TASKS + "/" + first.get("id").asText() + "/schedule")
                .contentType(MediaType.APPLICATION_JSON)
                .content(scheduleBody(MONDAY, "09:00", 0)))
        .andExpect(status().isOk());

    mvc.perform(
            put(TASKS + "/" + second.get("id").asText() + "/schedule")
                .contentType(MediaType.APPLICATION_JSON)
                .content(scheduleBody(MONDAY, "09:30", 0)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.schedule.startTime").value("09:30"));
  }

  @Test
  void FR_007_returns_conflict_with_candidate_and_performs_no_mutation() throws Exception {
    JsonNode blocking = createTask("Blocking", 60);
    JsonNode moving = createTask("Moving", 30);

    mvc.perform(
            put(TASKS + "/" + blocking.get("id").asText() + "/schedule")
                .contentType(MediaType.APPLICATION_JSON)
                .content(scheduleBody(MONDAY, "09:00", 0)))
        .andExpect(status().isOk());

    String payload =
        mvc.perform(
                put(TASKS + "/" + moving.get("id").asText() + "/schedule")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(scheduleBody(MONDAY, "09:30", 0)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("SCHEDULE_CONFLICT"))
            .andExpect(jsonPath("$.requestId").isNotEmpty())
            .andExpect(jsonPath("$.conflict.proposed.date").value(MONDAY))
            .andExpect(jsonPath("$.conflict.proposed.startTime").value("09:30"))
            .andExpect(jsonPath("$.conflict.proposed.endTime").value("10:00"))
            .andExpect(jsonPath("$.conflict.conflicting.startTime").value("09:00"))
            .andExpect(jsonPath("$.conflict.conflicting.endTime").value("10:00"))
            .andExpect(jsonPath("$.conflict.nextCandidate.startTime").value("10:00"))
            .andReturn()
            .getResponse()
            .getContentAsString();

    assertSafeErrorPayload(payload);
    assertThat(payload).doesNotContain("Blocking").doesNotContain("Moving");

    mvc.perform(get(TASKS + "/" + moving.get("id").asText()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.schedule").doesNotExist())
        .andExpect(jsonPath("$.version").value(0));
  }

  @Test
  void FR_007_accepting_the_next_candidate_succeeds() throws Exception {
    JsonNode blocking = createTask("Blocking", 60);
    JsonNode moving = createTask("Moving", 30);

    mvc.perform(
            put(TASKS + "/" + blocking.get("id").asText() + "/schedule")
                .contentType(MediaType.APPLICATION_JSON)
                .content(scheduleBody(MONDAY, "09:00", 0)))
        .andExpect(status().isOk());

    String conflict =
        mvc.perform(
                put(TASKS + "/" + moving.get("id").asText() + "/schedule")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(scheduleBody(MONDAY, "09:30", 0)))
            .andExpect(status().isConflict())
            .andReturn()
            .getResponse()
            .getContentAsString();

    JsonNode candidate = readJson(conflict).at("/conflict/nextCandidate");

    mvc.perform(
            put(TASKS + "/" + moving.get("id").asText() + "/schedule")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"date\":\"%s\",\"startTime\":\"%s\",\"expectedVersion\":0,\"resolutionMode\":\"ACCEPT_CANDIDATE\"}"
                        .formatted(
                            candidate.get("date").asText(), candidate.get("startTime").asText())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.schedule.startTime").value("10:00"));
  }

  @Test
  void FR_007_completed_task_does_not_block_a_new_schedule() throws Exception {
    JsonNode completed = createTask("Done", 30);
    JsonNode fresh = createTask("Fresh", 30);
    String completedId = completed.get("id").asText();

    mvc.perform(
            put(TASKS + "/" + completedId + "/schedule")
                .contentType(MediaType.APPLICATION_JSON)
                .content(scheduleBody(MONDAY, "09:00", 0)))
        .andExpect(status().isOk());
    mvc.perform(
            put(TASKS + "/" + completedId + "/completion")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"completed\":true,\"expectedVersion\":1}"))
        .andExpect(status().isOk());

    mvc.perform(
            put(TASKS + "/" + fresh.get("id").asText() + "/schedule")
                .contentType(MediaType.APPLICATION_JSON)
                .content(scheduleBody(MONDAY, "09:00", 0)))
        .andExpect(status().isOk());
  }

  @Test
  void FR_006_moves_an_already_scheduled_task() throws Exception {
    JsonNode task = createTask("Movable", 30);
    String id = task.get("id").asText();

    mvc.perform(
            put(TASKS + "/" + id + "/schedule")
                .contentType(MediaType.APPLICATION_JSON)
                .content(scheduleBody(MONDAY, "09:00", 0)))
        .andExpect(status().isOk());

    mvc.perform(
            put(TASKS + "/" + id + "/schedule")
                .contentType(MediaType.APPLICATION_JSON)
                .content(scheduleBody("2026-09-08", "14:00", 1)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.schedule.date").value("2026-09-08"))
        .andExpect(jsonPath("$.schedule.startTime").value("14:00"))
        .andExpect(jsonPath("$.version").value(2));

    assertThat(store.auditRecords()).contains("SCHEDULED:schedule", "MOVED:schedule");
  }

  @Test
  void FR_008_unschedules_task_and_preserves_content() throws Exception {
    JsonNode task = createTask("Preserved", 30, "HIGH", java.time.LocalDate.parse("2026-09-09"));
    String id = task.get("id").asText();

    mvc.perform(
            put(TASKS + "/" + id + "/schedule")
                .contentType(MediaType.APPLICATION_JSON)
                .content(scheduleBody(MONDAY, "09:00", 0)))
        .andExpect(status().isOk());

    mvc.perform(delete(TASKS + "/" + id + "/schedule").param("expectedVersion", "1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.schedule").doesNotExist())
        .andExpect(jsonPath("$.title").value("Preserved"))
        .andExpect(jsonPath("$.priority").value("HIGH"))
        .andExpect(jsonPath("$.estimateMinutes").value(30))
        .andExpect(jsonPath("$.dueDate").value("2026-09-09"))
        .andExpect(jsonPath("$.version").value(2));
  }

  @Test
  void FR_008_unschedule_is_idempotent() throws Exception {
    JsonNode task = createTask("Never scheduled", 30);

    mvc.perform(
            delete(TASKS + "/" + task.get("id").asText() + "/schedule")
                .param("expectedVersion", "0"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.schedule").doesNotExist())
        .andExpect(jsonPath("$.version").value(0));

    assertThat(store.auditRecords()).containsExactly("CREATED:task");
  }

  @Test
  void NFR_006_rejects_stale_schedule_command_without_mutation() throws Exception {
    JsonNode task = createTask("Stale schedule", 30);
    String id = task.get("id").asText();

    mvc.perform(
            put(TASKS + "/" + id + "/schedule")
                .contentType(MediaType.APPLICATION_JSON)
                .content(scheduleBody(MONDAY, "09:00", 4)))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("STALE_TASK"))
        .andExpect(jsonPath("$.currentVersion").value(0));

    mvc.perform(get(TASKS + "/" + id)).andExpect(jsonPath("$.schedule").doesNotExist());
  }

  @Test
  void FR_006_returns_safe_not_found_for_unknown_task_schedule() throws Exception {
    mvc.perform(
            put(TASKS + "/" + java.util.UUID.randomUUID() + "/schedule")
                .contentType(MediaType.APPLICATION_JSON)
                .content(scheduleBody(MONDAY, "09:00", 0)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("TASK_NOT_FOUND"));
  }
}
