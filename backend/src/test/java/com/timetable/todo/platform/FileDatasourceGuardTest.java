package com.timetable.todo.platform;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/**
 * NFR-003, SECURITY-01, SECURITY-12: The development file database must be encrypted and keyed from
 * the runtime environment.
 *
 * <p>There is deliberately no fallback key, so a misconfigured file profile fails startup instead
 * of silently writing an unencrypted database.
 */
class FileDatasourceGuardTest {

  private static final String ENCRYPTED_URL = "jdbc:h2:file:./data/planning;CIPHER=AES";

  @Test
  void SECURITY_12_rejects_a_missing_file_database_key() {
    assertThatThrownBy(() -> FileDatasourceGuard.validate(ENCRYPTED_URL, null))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("PLANNING_DB_PASSWORD");
  }

  @Test
  void SECURITY_12_rejects_a_blank_file_database_key() {
    assertThatThrownBy(() -> FileDatasourceGuard.validate(ENCRYPTED_URL, "   "))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void SECURITY_01_rejects_an_unencrypted_file_database() {
    assertThatThrownBy(
            () -> FileDatasourceGuard.validate("jdbc:h2:file:./data/planning", "file-key user-key"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("CIPHER=AES");
  }

  @Test
  void SECURITY_09_rejects_an_h2_tcp_or_console_style_url() {
    assertThatThrownBy(
            () ->
                FileDatasourceGuard.validate("jdbc:h2:tcp://localhost/planning;CIPHER=AES", "k u"))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void SECURITY_12_accepts_an_encrypted_url_with_a_runtime_key() {
    assertThatCode(() -> FileDatasourceGuard.validate(ENCRYPTED_URL, "file-key user-key"))
        .doesNotThrowAnyException();
  }

  @Test
  void SECURITY_03_failure_messages_never_contain_the_key() {
    String key = "super-secret-file-key";
    assertThatThrownBy(() -> FileDatasourceGuard.validate("jdbc:h2:file:./data/planning", key))
        .hasMessageNotContaining(key);
  }
}
