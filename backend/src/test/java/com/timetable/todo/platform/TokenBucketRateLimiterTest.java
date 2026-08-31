package com.timetable.todo.platform;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

/** SECURITY-11: The bucket is deterministic, per client and memory bounded. */
class TokenBucketRateLimiterTest {

  private static final Instant START = Instant.parse("2026-09-01T09:00:00Z");

  @Test
  void SECURITY_11_consumes_the_capacity_then_rejects() {
    TokenBucketRateLimiter limiter =
        new TokenBucketRateLimiter(2, 60, 10, Clock.fixed(START, ZoneOffset.UTC));

    assertThat(limiter.tryConsume("127.0.0.1")).isTrue();
    assertThat(limiter.tryConsume("127.0.0.1")).isTrue();
    assertThat(limiter.tryConsume("127.0.0.1")).isFalse();
  }

  @Test
  void SECURITY_11_tracks_each_client_separately() {
    TokenBucketRateLimiter limiter =
        new TokenBucketRateLimiter(1, 60, 10, Clock.fixed(START, ZoneOffset.UTC));

    assertThat(limiter.tryConsume("127.0.0.1")).isTrue();
    assertThat(limiter.tryConsume("127.0.0.2")).isTrue();
    assertThat(limiter.tryConsume("127.0.0.1")).isFalse();
  }

  @Test
  void SECURITY_11_refills_over_time_without_exceeding_capacity() {
    MutableClock clock = new MutableClock(START);
    TokenBucketRateLimiter limiter = new TokenBucketRateLimiter(2, 60, 10, clock);

    assertThat(limiter.tryConsume("client")).isTrue();
    assertThat(limiter.tryConsume("client")).isTrue();
    assertThat(limiter.tryConsume("client")).isFalse();

    clock.advance(Duration.ofMinutes(5));

    assertThat(limiter.tryConsume("client")).isTrue();
    assertThat(limiter.tryConsume("client")).isTrue();
    assertThat(limiter.tryConsume("client")).as("capacity is the ceiling").isFalse();
  }

  @Test
  void SECURITY_11_evicts_the_least_recently_used_client() {
    TokenBucketRateLimiter limiter =
        new TokenBucketRateLimiter(1, 1, 2, Clock.fixed(START, ZoneOffset.UTC));

    assertThat(limiter.tryConsume("a")).isTrue();
    assertThat(limiter.tryConsume("b")).isTrue();
    assertThat(limiter.tryConsume("c")).isTrue();
    assertThat(limiter.tryConsume("a")).as("evicted client starts with a fresh bucket").isTrue();
  }

  @Test
  void SECURITY_11_reports_a_positive_retry_delay() {
    TokenBucketRateLimiter limiter =
        new TokenBucketRateLimiter(1, 120, 10, Clock.fixed(START, ZoneOffset.UTC));

    assertThat(limiter.retryAfterSeconds()).isPositive();
  }

  private static final class MutableClock extends Clock {

    private Instant now;

    private MutableClock(Instant now) {
      this.now = now;
    }

    private void advance(Duration amount) {
      now = now.plus(amount);
    }

    @Override
    public ZoneOffset getZone() {
      return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(java.time.ZoneId zone) {
      return this;
    }

    @Override
    public Instant instant() {
      return now;
    }
  }
}
