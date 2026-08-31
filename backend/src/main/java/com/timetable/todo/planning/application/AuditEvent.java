package com.timetable.todo.planning.application;

import com.timetable.todo.planning.domain.TaskId;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** FR-013, NFR-003: Records structural changes without task content. */
public record AuditEvent(
    UUID id,
    TaskId taskId,
    AuditAction action,
    String actor,
    String requestId,
    Instant occurredAt,
    List<String> changedFields) {

  public AuditEvent {
    Objects.requireNonNull(id);
    Objects.requireNonNull(taskId);
    Objects.requireNonNull(action);
    Objects.requireNonNull(actor);
    Objects.requireNonNull(requestId);
    Objects.requireNonNull(occurredAt);
    changedFields = List.copyOf(changedFields);
  }
}
