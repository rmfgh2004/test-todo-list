package com.timetable.todo.planning.domain;

/** NFR-003: Represents a safe, typed domain validation failure. */
public final class DomainValidationException extends RuntimeException {

  private final String code;

  public DomainValidationException(String code, String message) {
    super(message);
    this.code = code;
  }

  public String code() {
    return code;
  }
}
