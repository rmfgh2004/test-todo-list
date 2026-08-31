package com.timetable.todo.planning.domain;

import java.util.Objects;
import java.util.UUID;

/** FR-003: Identifies a Task without exposing persistence concerns. */
public record TaskId(UUID value) {

  public TaskId {
    Objects.requireNonNull(value, "Task ID is required");
  }
}
