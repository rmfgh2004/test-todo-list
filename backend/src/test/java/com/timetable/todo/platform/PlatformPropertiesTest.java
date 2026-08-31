package com.timetable.todo.platform;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * NFR-003, SECURITY-08: Platform limits fall back to safe defaults and reject a wildcard origin.
 */
class PlatformPropertiesTest {

  @Test
  void NFR_003_absent_configuration_falls_back_to_conservative_defaults() {
    PlatformProperties properties = new PlatformProperties(null, null, null);

    assertThat(properties.allowedOrigins())
        .containsExactly("http://127.0.0.1:5173", "http://localhost:5173");
    assertThat(properties.maxRequestBodyBytes()).isEqualTo(65_536);
    assertThat(properties.rateLimit().capacity()).isEqualTo(120);
    assertThat(properties.rateLimit().refillPerMinute()).isEqualTo(120);
    assertThat(properties.rateLimit().maxClients()).isEqualTo(1_000);
  }

  @Test
  void NFR_003_an_empty_or_invalid_value_never_widens_a_boundary() {
    PlatformProperties properties =
        new PlatformProperties(List.of(), 0, new PlatformProperties.RateLimit(0, -5, 0));

    assertThat(properties.allowedOrigins()).hasSize(2);
    assertThat(properties.maxRequestBodyBytes()).isEqualTo(65_536);
    assertThat(properties.rateLimit().capacity()).isEqualTo(120);
  }

  @Test
  void SECURITY_08_rejects_a_wildcard_origin_at_startup() {
    assertThatThrownBy(() -> new PlatformProperties(List.of("*"), null, null))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("wildcard");
  }

  @Test
  void NFR_003_keeps_an_explicitly_configured_value() {
    PlatformProperties properties =
        new PlatformProperties(
            List.of("http://127.0.0.1:4173"), 1024, new PlatformProperties.RateLimit(5, 30, 10));

    assertThat(properties.allowedOrigins()).containsExactly("http://127.0.0.1:4173");
    assertThat(properties.maxRequestBodyBytes()).isEqualTo(1024);
    assertThat(properties.rateLimit().capacity()).isEqualTo(5);
  }
}
