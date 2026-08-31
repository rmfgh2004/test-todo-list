package com.timetable.todo.planning.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/** FR-006: Adapter-owned JPA representation of a Task schedule. */
@Entity
@Table(name = "schedule_slots")
class ScheduleSlotJpaEntity {

  @Id
  @Column(name = "task_id")
  private UUID taskId;

  @MapsId
  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "task_id")
  private TaskJpaEntity task;

  @Column(name = "schedule_date", nullable = false)
  private LocalDate date;

  @Column(name = "start_time", nullable = false)
  private LocalTime start;

  @Column(name = "end_time", nullable = false)
  private LocalTime end;

  protected ScheduleSlotJpaEntity() {}

  ScheduleSlotJpaEntity(TaskJpaEntity task, LocalDate date, LocalTime start, LocalTime end) {
    this.task = task;
    this.date = date;
    this.start = start;
    this.end = end;
  }

  void update(LocalDate date, LocalTime start, LocalTime end) {
    this.date = date;
    this.start = start;
    this.end = end;
  }

  LocalDate getDate() {
    return date;
  }

  LocalTime getStart() {
    return start;
  }

  LocalTime getEnd() {
    return end;
  }
}
