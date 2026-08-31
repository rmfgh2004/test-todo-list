package com.timetable.todo.planning.adapter.out.persistence;

import com.timetable.todo.planning.application.AuditAction;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** FR-013: Append-only persistence representation of structural audit data. */
@Entity
@Table(name = "audit_events")
class AuditEventJpaEntity {

  @Id private UUID id;

  @Column(name = "task_id", nullable = false)
  private UUID taskId;

  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.VARCHAR)
  @Column(nullable = false, length = 24)
  private AuditAction action;

  @Column(nullable = false, length = 64)
  private String actor;

  @Column(name = "request_id", nullable = false, length = 128)
  private String requestId;

  @Column(name = "occurred_at", nullable = false)
  private Instant occurredAt;

  @Column(name = "changed_fields", nullable = false, length = 512)
  private String changedFields;

  protected AuditEventJpaEntity() {}

  AuditEventJpaEntity(
      UUID id,
      UUID taskId,
      AuditAction action,
      String actor,
      String requestId,
      Instant occurredAt,
      String changedFields) {
    this.id = id;
    this.taskId = taskId;
    this.action = action;
    this.actor = actor;
    this.requestId = requestId;
    this.occurredAt = occurredAt;
    this.changedFields = changedFields;
  }

  AuditAction getAction() {
    return action;
  }

  String getChangedFields() {
    return changedFields;
  }
}
