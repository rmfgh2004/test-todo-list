package com.timetable.todo.planning.application;

/** NFR-006: Prevents a stale command from overwriting newer state. */
public final class StaleTaskVersionException extends RuntimeException {

  public StaleTaskVersionException() {
    super("Task version is stale");
  }
}
