package com.timetable.todo.platform;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * NFR-003: Typed platform limits with conservative defaults.
 *
 * <p>Defaults live here rather than in a configuration file so a missing or replaced {@code
 * application.yml} can never silently widen a boundary. No property has a secret default.
 */
@ConfigurationProperties(prefix = "planning.platform")
public record PlatformProperties(
    List<String> allowedOrigins, Integer maxRequestBodyBytes, RateLimit rateLimit) {

  private static final List<String> DEFAULT_ORIGINS =
      List.of("http://127.0.0.1:5173", "http://localhost:5173");
  private static final int DEFAULT_MAX_BODY_BYTES = 64 * 1024;

  public PlatformProperties {
    allowedOrigins =
        allowedOrigins == null || allowedOrigins.isEmpty()
            ? DEFAULT_ORIGINS
            : List.copyOf(allowedOrigins);
    maxRequestBodyBytes =
        maxRequestBodyBytes == null || maxRequestBodyBytes < 1
            ? DEFAULT_MAX_BODY_BYTES
            : maxRequestBodyBytes;
    rateLimit = rateLimit == null ? new RateLimit(null, null, null) : rateLimit;
    if (allowedOrigins.stream().anyMatch(origin -> origin.contains("*"))) {
      throw new IllegalStateException(
          "planning.platform.allowed-origins must not contain a wildcard");
    }
  }

  /** SECURITY-11: Bounded bucket policy; the client cache is capped so it cannot grow unbounded. */
  public record RateLimit(Integer capacity, Integer refillPerMinute, Integer maxClients) {

    public RateLimit {
      capacity = capacity == null || capacity < 1 ? 120 : capacity;
      refillPerMinute = refillPerMinute == null || refillPerMinute < 1 ? 120 : refillPerMinute;
      maxClients = maxClients == null || maxClients < 1 ? 1_000 : maxClients;
    }
  }
}
