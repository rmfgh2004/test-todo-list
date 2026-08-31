package com.timetable.todo.platform;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * NFR-003, SECURITY-11, SECURITY-15: Applies the bounded bucket before any command executes.
 *
 * <p>An evaluation failure fails closed for mutations, so a broken limiter can never become a way
 * to bypass the limit on a state-changing route.
 */
public class RateLimitFilter extends OncePerRequestFilter {

  private static final java.util.Set<String> SAFE_METHODS =
      java.util.Set.of("GET", "HEAD", "OPTIONS");

  private final TokenBucketRateLimiter limiter;
  private final SafeErrorWriter errors;

  RateLimitFilter(TokenBucketRateLimiter limiter, SafeErrorWriter errors) {
    this.limiter = limiter;
    this.errors = errors;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    boolean allowed;
    try {
      allowed = limiter.tryConsume(clientOf(request));
    } catch (RuntimeException failure) {
      allowed = SAFE_METHODS.contains(request.getMethod());
    }

    if (!allowed) {
      response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(limiter.retryAfterSeconds()));
      errors.write(
          response,
          HttpStatus.TOO_MANY_REQUESTS.value(),
          "RATE_LIMITED",
          "Too many requests; retry after the indicated delay");
      return;
    }
    chain.doFilter(request, response);
  }

  private String clientOf(HttpServletRequest request) {
    String address = request.getRemoteAddr();
    return address == null || address.isBlank() ? "unknown" : address;
  }
}
