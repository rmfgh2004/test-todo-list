package com.timetable.todo.platform;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * NFR-003, SECURITY-05: Rejects an oversized request body before any parsing or command work.
 *
 * <p>A request without a declared length is passed through; the container and the typed DTO bounds
 * still apply, so an unbounded body cannot reach the domain.
 */
public class RequestBodyLimitFilter extends OncePerRequestFilter {

  private final int maxBytes;
  private final SafeErrorWriter errors;

  RequestBodyLimitFilter(int maxBytes, SafeErrorWriter errors) {
    this.maxBytes = maxBytes;
    this.errors = errors;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    if (request.getContentLengthLong() > maxBytes) {
      errors.write(
          response,
          HttpStatus.PAYLOAD_TOO_LARGE.value(),
          "PAYLOAD_TOO_LARGE",
          "The request body exceeds the allowed size");
      return;
    }
    chain.doFilter(request, response);
  }
}
