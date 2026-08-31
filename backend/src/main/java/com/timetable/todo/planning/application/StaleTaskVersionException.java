package com.timetable.todo.planning.application;

/** NFR-006: Prevents a stale command from overwriting newer state. */
public final class StaleTaskVersionException extends RuntimeException {

  private final Long currentVersion;

  public StaleTaskVersionException() {
    this(null);
  }

  public StaleTaskVersionException(Long currentVersion) {
    super("Task version is stale");
    this.currentVersion = currentVersion;
  }

  /** Returns the persisted version a client must reload, or {@code null} when it is unknown. */
  public Long currentVersion() {
    return currentVersion;
  }
}
