package com.timetable.todo.planning.domain;

/** FR-003: Stores optional task description as untrusted plain text. */
public record TaskDescription(String value) {

  private static final int MAX_CODE_POINTS = 2_000;

  public static TaskDescription of(String rawValue) {
    if (rawValue == null) {
      return null;
    }
    String canonical = rawValue.strip();
    int length = canonical.codePointCount(0, canonical.length());
    if (length > MAX_CODE_POINTS) {
      throw new DomainValidationException(
          "TASK_DESCRIPTION_TOO_LONG", "Task description is too long");
    }
    return new TaskDescription(canonical);
  }
}
