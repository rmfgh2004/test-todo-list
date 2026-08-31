package com.timetable.todo.platform;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.regex.Pattern;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * NFR-008, SECURITY-03: Binds one opaque correlation ID to every request.
 *
 * <p>A client value is honoured only when it matches a strict opaque allowlist, so a header can
 * never inject control characters or unbounded text into logs. The MDC entry is always cleared, on
 * the success, rejection and exception paths alike.
 */
public class RequestIdFilter extends OncePerRequestFilter {

  static final String HEADER = "X-Request-Id";
  private static final Pattern ALLOWED = Pattern.compile("^[A-Za-z0-9._-]{8,64}$");

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    String supplied = request.getHeader(HEADER);
    String requestId =
        supplied != null && ALLOWED.matcher(supplied).matches()
            ? supplied
            : RequestCorrelation.generate();
    MDC.put(RequestCorrelation.MDC_KEY, requestId);
    response.setHeader(HEADER, requestId);
    try {
      chain.doFilter(request, response);
    } finally {
      MDC.remove(RequestCorrelation.MDC_KEY);
    }
  }
}
