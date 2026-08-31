package com.timetable.todo.planning.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.timetable.todo.planning.adapter.out.persistence.PlanningTestStore;
import com.timetable.todo.planning.adapter.out.persistence.PlanningTestStoreConfiguration;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * NFR-001, NFR-003: Shared MockMvc contract fixture for the planning REST adapter.
 *
 * <p>The ordered security platform chain is verified separately in Steps 11~12; these contract
 * tests assert the controller, DTO validation and safe error boundary with servlet filters
 * disabled.
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@Import(PlanningTestStoreConfiguration.class)
abstract class AbstractApiContractTest {

  protected static final String TASKS = "/api/v1/tasks";
  protected static final String WEEKS = "/api/v1/planning/weeks";
  protected static final LocalDate WEEK_START = LocalDate.parse("2026-09-07");

  @Autowired protected MockMvc mvc;
  @Autowired protected ObjectMapper json;
  @Autowired protected PlanningTestStore store;

  @BeforeEach
  void resetStore() {
    store.clear();
  }

  /** Creates a valid task and returns its response view. */
  protected JsonNode createTask(String title, int estimateMinutes) throws Exception {
    return createTask(title, estimateMinutes, "MEDIUM", null);
  }

  protected JsonNode createTask(
      String title, int estimateMinutes, String priority, LocalDate dueDate) throws Exception {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("title", title);
    body.put("priority", priority);
    body.put("estimateMinutes", estimateMinutes);
    if (dueDate != null) {
      body.put("dueDate", dueDate.toString());
    }
    String response =
        mvc.perform(
                post(TASKS)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json.writeValueAsString(body)))
            .andReturn()
            .getResponse()
            .getContentAsString();
    JsonNode view = json.readTree(response);
    assertThat(view.hasNonNull("id")).as("created task must expose an id").isTrue();
    return view;
  }

  protected JsonNode readJson(String payload) throws Exception {
    return json.readTree(payload);
  }

  /** NFR-003: Fails when an error payload leaks framework, path, query or stack detail. */
  protected void assertSafeErrorPayload(String payload) {
    assertThat(payload)
        .doesNotContain("org.springframework")
        .doesNotContain("org.hibernate")
        .doesNotContain("jakarta.")
        .doesNotContain("com.timetable")
        .doesNotContain("Exception")
        .doesNotContain("select ")
        .doesNotContain("SELECT ")
        .doesNotContain("/Users/");
  }
}
