package com.timetable.todo.platform;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/** NFR-003, SECURITY-11: A bounded token bucket rejects before any command runs. */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(
    properties = {
      "planning.platform.rate-limit.capacity=3",
      "planning.platform.rate-limit.refill-per-minute=3"
    })
class RateLimitTest {

  private static final String WEEK = "/api/v1/planning/weeks/2026-09-07";

  @Autowired private MockMvc mvc;

  @Test
  void SECURITY_11_rejects_with_retry_after_once_the_bucket_is_empty() throws Exception {
    for (int attempt = 0; attempt < 3; attempt++) {
      mvc.perform(get(WEEK)).andExpect(status().isOk());
    }

    mvc.perform(get(WEEK))
        .andExpect(status().isTooManyRequests())
        .andExpect(header().exists("Retry-After"))
        .andExpect(jsonPath("$.code").value("RATE_LIMITED"))
        .andExpect(jsonPath("$.requestId").isNotEmpty());
  }
}
