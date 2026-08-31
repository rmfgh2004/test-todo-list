package com.timetable.todo.platform;

/**
 * NFR-003, SECURITY-01, SECURITY-09, SECURITY-12: Fails startup on an unsafe file datasource.
 *
 * <p>There is deliberately no fallback key. A message never contains the supplied secret, only the
 * name of the environment variable an operator must set.
 */
public final class FileDatasourceGuard {

  private static final String KEY_VARIABLE = "PLANNING_DB_PASSWORD";

  private FileDatasourceGuard() {}

  public static void validate(String url, String password) {
    if (url == null || !url.startsWith("jdbc:h2:file:")) {
      throw new IllegalStateException(
          "The file profile requires a local jdbc:h2:file datasource; server or console URLs are rejected");
    }
    if (!url.toUpperCase().contains("CIPHER=AES")) {
      throw new IllegalStateException(
          "The file profile requires CIPHER=AES so the development database is encrypted at rest");
    }
    if (password == null || password.isBlank()) {
      throw new IllegalStateException(
          "The file profile requires the "
              + KEY_VARIABLE
              + " environment value; there is no default");
    }
  }
}
