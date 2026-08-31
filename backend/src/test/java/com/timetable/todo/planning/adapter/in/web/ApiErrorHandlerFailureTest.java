package com.timetable.todo.planning.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.timetable.todo.planning.application.PlanningService;
import com.timetable.todo.planning.domain.TaskId;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * NFR-008, SECURITY-15: An unexpected failure is sanitized before it can reach a client.
 *
 * <p>The service is replaced so a failure that no domain rule anticipates still produces the same
 * safe payload as a modelled failure.
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class ApiErrorHandlerFailureTest {

  @Autowired private MockMvc mvc;
  @MockitoBean private PlanningService planning;

  @Test
  void SECURITY_15_an_unexpected_failure_becomes_a_sanitized_500() throws Exception {
    given(planning.findById(any(TaskId.class)))
        .willThrow(new IllegalStateException("connection to jdbc:h2:file:/Users/secret failed"));

    String payload =
        mvc.perform(get("/api/v1/tasks/" + UUID.randomUUID()))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
            .andExpect(jsonPath("$.requestId").isNotEmpty())
            .andReturn()
            .getResponse()
            .getContentAsString();

    assertThat(payload)
        .doesNotContain("jdbc")
        .doesNotContain("/Users/")
        .doesNotContain("secret")
        .doesNotContain("IllegalState");
  }
}
