package com.timetable.todo.planning.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import tools.jackson.databind.JsonNode;

/** FR-003, FR-004, FR-009, FR-013, NFR-003: Task command contract over HTTP. */
class TaskCommandApiTest extends AbstractApiContractTest {

  @Test
  void FR_003_creates_task_and_returns_backlog_view() throws Exception {
    String body =
        """
        {"title":"Write report","description":"draft only","priority":"HIGH",\
        "estimateMinutes":45,"dueDate":"2026-09-10"}""";

    mvc.perform(post(TASKS).contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isCreated())
        .andExpect(header().exists("Location"))
        .andExpect(jsonPath("$.title").value("Write report"))
        .andExpect(jsonPath("$.description").value("draft only"))
        .andExpect(jsonPath("$.priority").value("HIGH"))
        .andExpect(jsonPath("$.estimateMinutes").value(45))
        .andExpect(jsonPath("$.dueDate").value("2026-09-10"))
        .andExpect(jsonPath("$.status").value("TODO"))
        .andExpect(jsonPath("$.schedule").doesNotExist())
        .andExpect(jsonPath("$.version").value(0))
        .andExpect(jsonPath("$.createdAt").isNotEmpty())
        .andExpect(jsonPath("$.updatedAt").isNotEmpty());
  }

  @Test
  void FR_003_creates_task_without_optional_fields() throws Exception {
    String body =
        """
        {"title":"Quick task","priority":"LOW","estimateMinutes":15}""";

    mvc.perform(post(TASKS).contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.description").doesNotExist())
        .andExpect(jsonPath("$.dueDate").doesNotExist());
  }

  @Test
  void FR_003_rejects_blank_title_with_safe_field_error() throws Exception {
    String body =
        """
        {"title":"   ","priority":"MEDIUM","estimateMinutes":30}""";

    String payload =
        mvc.perform(post(TASKS).contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
            .andExpect(jsonPath("$.requestId").isNotEmpty())
            .andExpect(jsonPath("$.fieldErrors[0].field").value("title"))
            .andExpect(jsonPath("$.fieldErrors[0].code").isNotEmpty())
            .andReturn()
            .getResponse()
            .getContentAsString();

    assertSafeErrorPayload(payload);
  }

  @Test
  void FR_003_rejects_estimate_outside_quarter_grid() throws Exception {
    String body =
        """
        {"title":"Bad estimate","priority":"MEDIUM","estimateMinutes":20}""";

    mvc.perform(post(TASKS).contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
        .andExpect(jsonPath("$.fieldErrors[0].field").value("estimateMinutes"));
  }

  @Test
  void FR_003_rejects_estimate_above_daily_maximum() throws Exception {
    String body =
        """
        {"title":"Too long","priority":"MEDIUM","estimateMinutes":900}""";

    mvc.perform(post(TASKS).contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.fieldErrors[0].field").value("estimateMinutes"));
  }

  @Test
  void FR_003_rejects_priority_outside_allowlist() throws Exception {
    String body =
        """
        {"title":"Bad priority","priority":"URGENT","estimateMinutes":30}""";

    String payload =
        mvc.perform(post(TASKS).contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"))
            .andReturn()
            .getResponse()
            .getContentAsString();

    assertThat(payload).doesNotContain("URGENT");
    assertSafeErrorPayload(payload);
  }

  @Test
  void FR_004_returns_task_detail_by_id() throws Exception {
    JsonNode created = createTask("Detail task", 30, "MEDIUM", LocalDate.parse("2026-09-09"));

    mvc.perform(get(TASKS + "/" + created.get("id").asText()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(created.get("id").asText()))
        .andExpect(jsonPath("$.title").value("Detail task"))
        .andExpect(jsonPath("$.dueDate").value("2026-09-09"));
  }

  @Test
  void FR_004_returns_safe_not_found_for_unknown_task() throws Exception {
    String payload =
        mvc.perform(get(TASKS + "/" + UUID.randomUUID()))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("TASK_NOT_FOUND"))
            .andExpect(jsonPath("$.requestId").isNotEmpty())
            .andReturn()
            .getResponse()
            .getContentAsString();

    assertSafeErrorPayload(payload);
  }

  @Test
  void FR_004_rejects_malformed_task_id() throws Exception {
    String payload =
        mvc.perform(get(TASKS + "/not-a-uuid"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
            .andReturn()
            .getResponse()
            .getContentAsString();

    assertThat(payload).doesNotContain("not-a-uuid");
    assertSafeErrorPayload(payload);
  }

  @Test
  void FR_004_deletes_task_after_explicit_confirmation() throws Exception {
    JsonNode created = createTask("Removable", 30);
    String id = created.get("id").asText();

    mvc.perform(delete(TASKS + "/" + id).param("expectedVersion", "0").param("confirmed", "true"))
        .andExpect(status().isNoContent());

    mvc.perform(get(TASKS + "/" + id)).andExpect(status().isNotFound());
  }

  @Test
  void FR_004_rejects_delete_without_confirmation() throws Exception {
    JsonNode created = createTask("Protected", 30);
    String id = created.get("id").asText();

    mvc.perform(delete(TASKS + "/" + id).param("expectedVersion", "0").param("confirmed", "false"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("DELETION_NOT_CONFIRMED"));

    mvc.perform(get(TASKS + "/" + id)).andExpect(status().isOk());
  }

  @Test
  void FR_004_requires_expected_version_for_delete() throws Exception {
    JsonNode created = createTask("Version required", 30);

    mvc.perform(delete(TASKS + "/" + created.get("id").asText()).param("confirmed", "true"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
  }

  @Test
  void NFR_006_rejects_stale_delete_with_current_version() throws Exception {
    JsonNode created = createTask("Stale delete", 30);

    mvc.perform(
            delete(TASKS + "/" + created.get("id").asText())
                .param("expectedVersion", "7")
                .param("confirmed", "true"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("STALE_TASK"))
        .andExpect(jsonPath("$.currentVersion").value(0))
        .andExpect(jsonPath("$.requestId").isNotEmpty());
  }

  @Test
  void FR_009_completes_and_reopens_task() throws Exception {
    JsonNode created = createTask("Completable", 30);
    String id = created.get("id").asText();

    mvc.perform(
            put(TASKS + "/" + id + "/completion")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"completed\":true,\"expectedVersion\":0}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("COMPLETED"))
        .andExpect(jsonPath("$.version").value(1));

    mvc.perform(
            put(TASKS + "/" + id + "/completion")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"completed\":false,\"expectedVersion\":1}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("TODO"))
        .andExpect(jsonPath("$.version").value(2));
  }

  @Test
  void FR_009_completion_command_is_idempotent() throws Exception {
    JsonNode created = createTask("Idempotent", 30);
    String id = created.get("id").asText();

    mvc.perform(
            put(TASKS + "/" + id + "/completion")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"completed\":true,\"expectedVersion\":0}"))
        .andExpect(status().isOk());

    mvc.perform(
            put(TASKS + "/" + id + "/completion")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"completed\":true,\"expectedVersion\":1}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("COMPLETED"))
        .andExpect(jsonPath("$.version").value(1));

    assertThat(store.auditCount()).as("no duplicate audit event for a no-op").isEqualTo(2);
  }

  @Test
  void FR_013_audit_events_record_structure_without_task_content() throws Exception {
    createTask("Very secret meeting title", 30);

    assertThat(store.auditRecords())
        .singleElement()
        .satisfies(
            record -> {
              assertThat(record).startsWith("CREATED:");
              assertThat(record).doesNotContain("Very secret meeting title");
            });
  }
}
