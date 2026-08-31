package com.timetable.todo.platform;

import com.timetable.todo.planning.adapter.in.web.ApiError;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * NFR-003, NFR-008: Writes the same safe error shape from a filter that the controller advice uses.
 *
 * <p>A request rejected before the dispatcher still answers with an allowlisted code, an authored
 * message and the correlation ID, so a client never has to parse a container error page.
 */
@Component
public class SafeErrorWriter {

  private final ObjectMapper json;

  SafeErrorWriter(ObjectMapper json) {
    this.json = json;
  }

  public void write(HttpServletResponse response, int status, String code, String message)
      throws IOException {
    response.setStatus(status);
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
    response
        .getWriter()
        .write(json.writeValueAsString(ApiError.of(code, message, RequestCorrelation.current())));
  }
}
