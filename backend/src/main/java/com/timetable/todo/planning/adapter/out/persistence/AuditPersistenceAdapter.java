package com.timetable.todo.planning.adapter.out.persistence;

import com.timetable.todo.planning.application.AuditEvent;
import com.timetable.todo.planning.application.AuditPort;
import org.springframework.stereotype.Repository;

/** FR-013: Implements append-only structural audit persistence. */
@Repository
public class AuditPersistenceAdapter implements AuditPort {

  private final AuditEventJpaRepository repository;

  AuditPersistenceAdapter(AuditEventJpaRepository repository) {
    this.repository = repository;
  }

  @Override
  public void append(AuditEvent event) {
    repository.save(
        new AuditEventJpaEntity(
            event.id(),
            event.taskId().value(),
            event.action(),
            event.actor(),
            event.requestId(),
            event.occurredAt(),
            String.join(",", event.changedFields())));
  }
}
