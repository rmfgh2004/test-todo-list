package com.timetable.todo.planning.application;

/** FR-004: Requires an explicit destructive-action contract. */
public final class DeletionNotConfirmedException extends RuntimeException {

  public DeletionNotConfirmedException() {
    super("Task deletion was not confirmed");
  }
}
