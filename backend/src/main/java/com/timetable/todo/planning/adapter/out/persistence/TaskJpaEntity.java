package com.timetable.todo.planning.adapter.out.persistence;

import com.timetable.todo.planning.domain.Priority;
import com.timetable.todo.planning.domain.TaskStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** FR-012: Adapter-owned JPA representation of Task. */
@Entity
@Table(name = "tasks")
class TaskJpaEntity {

  @Id private UUID id;

  @Column(nullable = false, length = 120)
  private String title;

  @Column(length = 2000)
  private String description;

  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.VARCHAR)
  @Column(nullable = false, length = 16)
  private Priority priority;

  @Column(name = "estimate_minutes", nullable = false)
  private int estimateMinutes;

  @Column(name = "due_date")
  private LocalDate dueDate;

  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.VARCHAR)
  @Column(nullable = false, length = 16)
  private TaskStatus status;

  @Version
  @Column(nullable = false)
  private long version;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @OneToOne(
      mappedBy = "task",
      cascade = CascadeType.ALL,
      orphanRemoval = true,
      fetch = FetchType.LAZY)
  private ScheduleSlotJpaEntity schedule;

  protected TaskJpaEntity() {}

  TaskJpaEntity(
      UUID id,
      String title,
      String description,
      Priority priority,
      int estimateMinutes,
      LocalDate dueDate,
      TaskStatus status,
      Instant createdAt,
      Instant updatedAt) {
    this.id = id;
    this.title = title;
    this.description = description;
    this.priority = priority;
    this.estimateMinutes = estimateMinutes;
    this.dueDate = dueDate;
    this.status = status;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
  }

  void updateFrom(com.timetable.todo.planning.domain.Task source) {
    title = source.title().value();
    description = source.description().map(value -> value.value()).orElse(null);
    priority = source.priority();
    estimateMinutes = source.estimate().value();
    dueDate = source.dueDate().orElse(null);
    status = source.status();
    updatedAt = source.updatedAt();
    source.schedule().ifPresentOrElse(this::setSchedule, this::clearSchedule);
  }

  private void setSchedule(com.timetable.todo.planning.domain.ScheduleSlot slot) {
    if (schedule == null) {
      schedule = new ScheduleSlotJpaEntity(this, slot.date(), slot.start(), slot.end());
    } else {
      schedule.update(slot.date(), slot.start(), slot.end());
    }
  }

  private void clearSchedule() {
    schedule = null;
  }

  UUID getId() {
    return id;
  }

  String getTitle() {
    return title;
  }

  String getDescription() {
    return description;
  }

  Priority getPriority() {
    return priority;
  }

  int getEstimateMinutes() {
    return estimateMinutes;
  }

  LocalDate getDueDate() {
    return dueDate;
  }

  TaskStatus getStatus() {
    return status;
  }

  long getVersion() {
    return version;
  }

  Instant getCreatedAt() {
    return createdAt;
  }

  Instant getUpdatedAt() {
    return updatedAt;
  }

  ScheduleSlotJpaEntity getSchedule() {
    return schedule;
  }
}
