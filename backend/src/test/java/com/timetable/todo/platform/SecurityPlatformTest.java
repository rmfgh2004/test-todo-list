package com.timetable.todo.platform;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * NFR-003, NFR-008: The ordered HTTP platform chain with every filter active.
 *
 * <p>The REST contract tests deliberately run with filters disabled, so this class is the only
 * place that proves correlation, CORS, headers, body limits and route authorization actually apply.
 */
@SpringBootTest
@AutoConfigureMockMvc
class SecurityPlatformTest {

  private static final String WEEK = "/api/v1/planning/weeks/2026-09-07";

  @Autowired private MockMvc mvc;

  @Test
  void NFR_008_every_response_carries_a_server_request_id() throws Exception {
    String requestId =
        mvc.perform(get(WEEK))
            .andExpect(status().isOk())
            .andExpect(header().exists("X-Request-Id"))
            .andReturn()
            .getResponse()
            .getHeader("X-Request-Id");

    assertThat(requestId).isNotBlank().hasSizeGreaterThanOrEqualTo(8);
  }

  @Test
  void NFR_008_accepts_a_well_formed_client_request_id() throws Exception {
    mvc.perform(get(WEEK).header("X-Request-Id", "client-correlation-01"))
        .andExpect(status().isOk())
        .andExpect(header().string("X-Request-Id", "client-correlation-01"));
  }

  @Test
  void SECURITY_03_replaces_a_client_request_id_outside_the_allowlist() throws Exception {
    String injected = "<script>alert(1)</script>";

    String requestId =
        mvc.perform(get(WEEK).header("X-Request-Id", injected))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getHeader("X-Request-Id");

    assertThat(requestId).isNotNull().isNotEqualTo(injected).doesNotContain("<");
  }

  @Test
  void SECURITY_03_replaces_an_oversized_client_request_id() throws Exception {
    String oversized = "a".repeat(200);

    String requestId =
        mvc.perform(get(WEEK).header("X-Request-Id", oversized))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getHeader("X-Request-Id");

    assertThat(requestId).isNotNull().hasSizeLessThanOrEqualTo(64);
  }

  @Test
  void NFR_008_clears_the_correlation_context_after_the_request() throws Exception {
    mvc.perform(get(WEEK)).andExpect(status().isOk());

    assertThat(MDC.get(RequestCorrelation.MDC_KEY))
        .as("MDC must not leak between requests")
        .isNull();
  }

  @Test
  void SECURITY_04_applies_the_baseline_response_headers() throws Exception {
    mvc.perform(get(WEEK))
        .andExpect(status().isOk())
        .andExpect(header().string("X-Content-Type-Options", "nosniff"))
        .andExpect(header().string("X-Frame-Options", "DENY"))
        .andExpect(header().string("Referrer-Policy", "no-referrer"))
        .andExpect(header().exists("Content-Security-Policy"));
  }

  @Test
  void SECURITY_04_does_not_claim_transport_security_over_plain_http() throws Exception {
    mvc.perform(get(WEEK))
        .andExpect(status().isOk())
        .andExpect(header().doesNotExist("Strict-Transport-Security"));
  }

  @Test
  void SECURITY_08_allows_the_declared_loopback_origin() throws Exception {
    mvc.perform(
            options(WEEK)
                .header(HttpHeaders.ORIGIN, "http://127.0.0.1:5173")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
        .andExpect(status().isOk())
        .andExpect(header().string("Access-Control-Allow-Origin", "http://127.0.0.1:5173"));
  }

  @Test
  void SECURITY_08_rejects_an_undeclared_origin() throws Exception {
    mvc.perform(
            options(WEEK)
                .header(HttpHeaders.ORIGIN, "https://attacker.example")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
        .andExpect(status().isForbidden())
        .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
  }

  @Test
  void SECURITY_08_denies_a_route_that_is_not_explicitly_public() throws Exception {
    mvc.perform(get("/internal/anything")).andExpect(status().isForbidden());
  }

  @Test
  void SECURITY_08_serves_the_declared_documentation_routes() throws Exception {
    mvc.perform(get("/docs/index.html")).andExpect(status().isOk());
    mvc.perform(get("/openapi/planning-api.yaml")).andExpect(status().isOk());
  }

  @Test
  void SECURITY_05_rejects_a_request_body_above_the_configured_limit() throws Exception {
    String oversized =
        "{\"title\":\"%s\",\"priority\":\"LOW\",\"estimateMinutes\":30}"
            .formatted("x".repeat(70_000));

    mvc.perform(post("/api/v1/tasks").contentType(MediaType.APPLICATION_JSON).content(oversized))
        .andExpect(status().isPayloadTooLarge())
        .andExpect(jsonPath("$.code").value("PAYLOAD_TOO_LARGE"))
        .andExpect(jsonPath("$.requestId").isNotEmpty());
  }

  @Test
  void SECURITY_14_exposes_only_a_sanitized_health_endpoint() throws Exception {
    mvc.perform(get("/actuator/health"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("UP"))
        .andExpect(jsonPath("$.components").doesNotExist());
  }

  @Test
  void SECURITY_14_does_not_expose_other_management_endpoints() throws Exception {
    for (String endpoint : new String[] {"env", "beans", "mappings", "configprops", "loggers"}) {
      mvc.perform(get("/actuator/" + endpoint))
          .andExpect(result -> assertThat(result.getResponse().getStatus()).isIn(403, 404));
    }
  }

  @Test
  void SECURITY_09_does_not_expose_an_h2_console() throws Exception {
    mvc.perform(get("/h2-console"))
        .andExpect(result -> assertThat(result.getResponse().getStatus()).isIn(403, 404));
  }
}
