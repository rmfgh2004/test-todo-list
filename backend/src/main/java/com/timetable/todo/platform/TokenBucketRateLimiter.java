package com.timetable.todo.platform;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * NFR-003, SECURITY-11: A bounded per-client token bucket.
 *
 * <p>The client cache is a fixed-size LRU map, so a flood of distinct clients cannot grow memory
 * without limit. The clock is injected so the policy is deterministic under test.
 */
public class TokenBucketRateLimiter {

  private final int capacity;
  private final double tokensPerSecond;
  private final Clock clock;
  private final Map<String, Bucket> buckets;

  public TokenBucketRateLimiter(int capacity, int refillPerMinute, int maxClients, Clock clock) {
    this.capacity = capacity;
    this.tokensPerSecond = refillPerMinute / 60.0;
    this.clock = Objects.requireNonNull(clock);
    this.buckets =
        new LinkedHashMap<>(16, 0.75f, true) {
          @Override
          protected boolean removeEldestEntry(Map.Entry<String, Bucket> eldest) {
            return size() > maxClients;
          }
        };
  }

  /** Returns true when the caller may proceed and consumes one token. */
  public synchronized boolean tryConsume(String client) {
    Instant now = clock.instant();
    Bucket bucket = buckets.computeIfAbsent(client, ignored -> new Bucket(capacity, now));
    bucket.refill(now, capacity, tokensPerSecond);
    return bucket.consume();
  }

  /** Seconds a rejected caller should wait before the next token is available. */
  public long retryAfterSeconds() {
    return Math.max(1, (long) Math.ceil(1 / tokensPerSecond));
  }

  private static final class Bucket {

    private double tokens;
    private Instant updatedAt;

    private Bucket(int initialTokens, Instant now) {
      this.tokens = initialTokens;
      this.updatedAt = now;
    }

    private void refill(Instant now, int capacity, double tokensPerSecond) {
      long elapsedMillis = Duration.between(updatedAt, now).toMillis();
      if (elapsedMillis > 0) {
        tokens = Math.min(capacity, tokens + (elapsedMillis / 1000.0) * tokensPerSecond);
        updatedAt = now;
      }
    }

    private boolean consume() {
      if (tokens < 1) {
        return false;
      }
      tokens -= 1;
      return true;
    }
  }
}
