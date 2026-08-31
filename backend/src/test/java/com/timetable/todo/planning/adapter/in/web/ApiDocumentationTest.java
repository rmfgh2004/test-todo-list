package com.timetable.todo.planning.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

/**
 * NFR-007: The locally served API documentation exposes the checked-in contract byte for byte.
 *
 * <p>Swagger UI assets are served from this application only, so the page needs no CDN script and
 * stays usable under a strict local CSP (SECURITY-04).
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class ApiDocumentationTest {

  @Autowired private MockMvc mvc;

  @Test
  void NFR_007_serves_the_checked_in_openapi_contract() throws Exception {
    String published =
        mvc.perform(get("/openapi/planning-api.yaml"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString(StandardCharsets.UTF_8);

    assertThat(published)
        .isEqualTo(
            Files.readString(Path.of("openapi", "planning-api.yaml"), StandardCharsets.UTF_8));
  }

  @Test
  void NFR_007_serves_a_self_contained_documentation_page() throws Exception {
    String page =
        mvc.perform(get("/docs/index.html"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString(StandardCharsets.UTF_8);

    String initializer =
        mvc.perform(get("/docs/swagger-initializer.js"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString(StandardCharsets.UTF_8);

    assertThat(page).contains("/webjars/swagger-ui/");
    assertThat(initializer).contains("/openapi/planning-api.yaml");
    assertThat(page + initializer).doesNotContain("http://").doesNotContain("https://");
  }

  @Test
  void SECURITY_04_swagger_assets_are_served_from_this_application() throws Exception {
    mvc.perform(get("/webjars/swagger-ui/5.29.4/swagger-ui-bundle.js")).andExpect(status().isOk());
    mvc.perform(get("/webjars/swagger-ui/5.29.4/swagger-ui.css")).andExpect(status().isOk());
  }
}
