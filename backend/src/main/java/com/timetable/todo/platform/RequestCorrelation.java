package com.timetable.todo.platform;

import java.util.UUID;
import org.slf4j.MDC;

/**
 * NFR-008, SECURITY-03: Single source of the opaque request correlation ID.
 *
 * <p>The ordered platform filter chain populates the MDC entry; until then, and for any request
 * that bypasses it, a fresh server-generated value is used so every response still carries an ID.
 */
public final class RequestCorrelation {

  public static final String MDC_KEY = "requestId";

  private RequestCorrelation() {}

  /** Returns the correlation ID of the current request, generating one when none is bound. */
  public static String current() {
    String bound = MDC.get(MDC_KEY);
    return bound == null || bound.isBlank() ? generate() : bound;
  }

  public static String generate() {
    return UUID.randomUUID().toString();
  }
}
