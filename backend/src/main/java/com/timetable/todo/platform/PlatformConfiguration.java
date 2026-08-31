package com.timetable.todo.platform;

import java.time.Clock;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/**
 * NFR-003: Fixes the ordered HTTP boundary.
 *
 * <p>Correlation runs first so every later rejection is traceable, then the body limit, then the
 * bounded rate limit, and only afterwards the Spring Security chain and the dispatcher.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(PlatformProperties.class)
public class PlatformConfiguration {

  static final int REQUEST_ID_ORDER = Ordered.HIGHEST_PRECEDENCE;
  static final int BODY_LIMIT_ORDER = Ordered.HIGHEST_PRECEDENCE + 10;
  static final int RATE_LIMIT_ORDER = Ordered.HIGHEST_PRECEDENCE + 20;

  @Bean
  FilterRegistrationBean<RequestIdFilter> requestIdFilter() {
    return ordered(new RequestIdFilter(), REQUEST_ID_ORDER);
  }

  @Bean
  FilterRegistrationBean<RequestBodyLimitFilter> requestBodyLimitFilter(
      PlatformProperties properties, SafeErrorWriter errors) {
    return ordered(
        new RequestBodyLimitFilter(properties.maxRequestBodyBytes(), errors), BODY_LIMIT_ORDER);
  }

  @Bean
  TokenBucketRateLimiter rateLimiter(PlatformProperties properties, Clock clock) {
    PlatformProperties.RateLimit policy = properties.rateLimit();
    return new TokenBucketRateLimiter(
        policy.capacity(), policy.refillPerMinute(), policy.maxClients(), clock);
  }

  @Bean
  FilterRegistrationBean<RateLimitFilter> rateLimitFilter(
      TokenBucketRateLimiter limiter, SafeErrorWriter errors) {
    return ordered(new RateLimitFilter(limiter, errors), RATE_LIMIT_ORDER);
  }

  private <T extends jakarta.servlet.Filter> FilterRegistrationBean<T> ordered(
      T filter, int order) {
    FilterRegistrationBean<T> registration = new FilterRegistrationBean<>(filter);
    registration.setOrder(order);
    registration.addUrlPatterns("/*");
    return registration;
  }
}
