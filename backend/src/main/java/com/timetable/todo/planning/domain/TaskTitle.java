package com.timetable.todo.planning.domain;

/** FR-003: Stores canonical task title text. */
public record TaskTitle(String value) {

  private static final int MAX_CODE_POINTS = 120;

  public static TaskTitle of(String rawValue) {
    if (rawValue == null) {
      throw invalid();
    }
    String canonical = rawValue.strip();
    int length = canonical.codePointCount(0, canonical.length());
    if (canonical.isBlank() || length > MAX_CODE_POINTS) {
      throw invalid();
    }
    return new TaskTitle(canonical);
  }

  private static DomainValidationException invalid() {
    return new DomainValidationException("TASK_TITLE_INVALID", "Task title is invalid");
  }
}
