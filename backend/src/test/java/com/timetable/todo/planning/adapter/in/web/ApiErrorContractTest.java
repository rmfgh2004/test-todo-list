package com.timetable.todo.planning.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import tools.jackson.databind.JsonNode;

/** NFR-003, NFR-004, NFR-008: Safe and semantic error contract of the planning API. */
class ApiErrorContractTest extends AbstractApiContractTest {

  @Test
  void NFR_003_rejects_unsupported_media_type() throws Exception {
    String payload =
        mvc.perform(post(TASKS).contentType(MediaType.TEXT_PLAIN).content("title=hack"))
            .andExpect(status().isUnsupportedMediaType())
            .andExpect(jsonPath("$.code").value("UNSUPPORTED_MEDIA_TYPE"))
            .andExpect(jsonPath("$.requestId").isNotEmpty())
            .andReturn()
            .getResponse()
            .getContentAsString();

    assertSafeErrorPayload(payload);
  }

  @Test
  void NFR_003_rejects_malformed_json_without_parser_detail() throws Exception {
    String payload =
        mvc.perform(
                post(TASKS)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"title\":\"broken\","))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"))
            .andReturn()
            .getResponse()
            .getContentAsString();

    assertThat(payload)
        .doesNotContain("line")
        .doesNotContain("column")
        .doesNotContain("JSON parse")
        .doesNotContain("broken");
    assertSafeErrorPayload(payload);
  }

  @Test
  void NFR_003_rejects_oversized_title_without_echoing_input() throws Exception {
    String oversized = "z".repeat(121);
    String body =
        "{\"title\":\"%s\",\"priority\":\"MEDIUM\",\"estimateMinutes\":30}".formatted(oversized);

    String payload =
        mvc.perform(post(TASKS).contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
            .andExpect(jsonPath("$.fieldErrors[0].field").value("title"))
            .andReturn()
            .getResponse()
            .getContentAsString();

    assertThat(payload).doesNotContain(oversized).doesNotContain("zzzzzzzzzz");
    assertSafeErrorPayload(payload);
  }

  @Test
  void NFR_003_rejects_oversized_description_without_echoing_input() throws Exception {
    String oversized = "y".repeat(2001);
    String body =
        "{\"title\":\"ok\",\"description\":\"%s\",\"priority\":\"MEDIUM\",\"estimateMinutes\":30}"
            .formatted(oversized);

    String payload =
        mvc.perform(post(TASKS).contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.fieldErrors[0].field").value("description"))
            .andReturn()
            .getResponse()
            .getContentAsString();

    assertThat(payload).doesNotContain("yyyyyyyyyy");
  }

  @Test
  void NFR_003_returns_safe_not_found_for_an_unmatched_route() throws Exception {
    String payload =
        mvc.perform(get("/api/v1/does-not-exist"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
            .andExpect(jsonPath("$.requestId").isNotEmpty())
            .andReturn()
            .getResponse()
            .getContentAsString();

    assertThat(payload).doesNotContain("does-not-exist");
    assertSafeErrorPayload(payload);
  }

  @Test
  void NFR_003_rejects_unsupported_method_on_a_known_route() throws Exception {
    mvc.perform(put(TASKS).contentType(MediaType.APPLICATION_JSON).content("{}"))
        .andExpect(status().isMethodNotAllowed())
        .andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"))
        .andExpect(jsonPath("$.requestId").isNotEmpty());
  }

  @Test
  void NFR_003_rejects_missing_request_body() throws Exception {
    mvc.perform(post(TASKS).contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
  }

  @Test
  void NFR_008_every_error_response_carries_a_request_id() throws Exception {
    String payload =
        mvc.perform(get(TASKS + "/" + UUID.randomUUID()))
            .andExpect(status().isNotFound())
            .andReturn()
            .getResponse()
            .getContentAsString();

    assertThat(readJson(payload).get("requestId").asText()).isNotBlank();
  }

  @Test
  void NFR_004_error_payload_keeps_the_documented_field_set() throws Exception {
    String payload =
        mvc.perform(
                post(TASKS)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"title\":\"\",\"priority\":\"MEDIUM\",\"estimateMinutes\":30}"))
            .andExpect(status().isBadRequest())
            .andReturn()
            .getResponse()
            .getContentAsString();

    JsonNode error = readJson(payload);
    assertThat(error.has("code")).isTrue();
    assertThat(error.has("message")).isTrue();
    assertThat(error.has("requestId")).isTrue();
    assertThat(error.has("fieldErrors")).isTrue();
    assertThat(error.size()).as("error payload exposes no extra field").isEqualTo(4);
  }

  @Test
  void NFR_003_error_responses_use_the_json_problem_free_api_content_type() throws Exception {
    mvc.perform(get(TASKS + "/" + UUID.randomUUID()))
        .andExpect(status().isNotFound())
        .andExpect(
            result ->
                assertThat(result.getResponse().getContentType()).startsWith("application/json"));
  }
}
