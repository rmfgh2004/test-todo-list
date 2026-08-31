package com.timetable.todo.planning.application;

/** FR-004: Signals a safe missing-task outcome. */
public final class TaskNotFoundException extends RuntimeException {

  public TaskNotFoundException() {
    super("Task was not found");
  }
}
