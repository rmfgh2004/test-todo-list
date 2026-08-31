package com.timetable.todo.planning.adapter.out.persistence;

import java.util.List;

/**
 * NFR-001: Gives contract tests a narrow, package-respecting view of persisted planning state.
 *
 * <p>The JPA repositories stay package-private so no test outside the persistence adapter can bind
 * to Hibernate entities directly.
 */
public class PlanningTestStore {

  private final TaskJpaRepository tasks;
  private final AuditEventJpaRepository audits;

  PlanningTestStore(TaskJpaRepository tasks, AuditEventJpaRepository audits) {
    this.tasks = tasks;
    this.audits = audits;
  }

  /** Removes every persisted planning row so each contract test starts from a known state. */
  public void clear() {
    audits.deleteAll();
    tasks.deleteAll();
  }

  public long auditCount() {
    return audits.count();
  }

  /** FR-013: Returns only the structural audit projection a test may assert on. */
  public List<String> auditRecords() {
    return audits.findAll().stream()
        .map(event -> event.getAction().name() + ":" + event.getChangedFields())
        .toList();
  }
}
