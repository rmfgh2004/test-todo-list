package com.timetable.todo.planning.application;

/** FR-013: Exposes append-only audit persistence. */
public interface AuditPort {

  void append(AuditEvent event);
}
