package com.timetable.todo.planning.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

/** FR-003, FR-004, FR-006, FR-008, FR-009: Owns valid task content and schedule state. */
public final class Task {

  private final TaskId id;
  private final TaskTitle title;
  private final TaskDescription description;
  private final Priority priority;
  private final EstimateMinutes estimate;
  private final LocalDate dueDate;
  private final TaskStatus status;
  private final ScheduleSlot schedule;
  private final long version;
  private final Instant createdAt;
  private final Instant updatedAt;

  private Task(
      TaskId id,
      TaskTitle title,
      TaskDescription description,
      Priority priority,
      EstimateMinutes estimate,
      LocalDate dueDate,
      TaskStatus status,
      ScheduleSlot schedule,
      long version,
      Instant createdAt,
      Instant updatedAt) {
    this.id = Objects.requireNonNull(id, "Task ID is required");
    this.title = Objects.requireNonNull(title, "Task title is required");
    this.description = description;
    this.priority = Objects.requireNonNull(priority, "Task priority is required");
    this.estimate = Objects.requireNonNull(estimate, "Task estimate is required");
    this.dueDate = dueDate;
    this.status = Objects.requireNonNull(status, "Task status is required");
    this.schedule = schedule;
    this.version = version;
    this.createdAt = Objects.requireNonNull(createdAt, "Created time is required");
    this.updatedAt = Objects.requireNonNull(updatedAt, "Updated time is required");
  }

  public static Task create(
      TaskId id,
      String title,
      String description,
      Priority priority,
      int estimateMinutes,
      LocalDate dueDate,
      Instant now) {
    return new Task(
        id,
        TaskTitle.of(title),
        TaskDescription.of(description),
        priority,
        EstimateMinutes.of(estimateMinutes),
        dueDate,
        TaskStatus.TODO,
        null,
        0,
        now,
        now);
  }

  /** NFR-007: Rehydrates a previously validated aggregate at the persistence adapter boundary. */
  public static Task rehydrate(
      TaskId id,
      TaskTitle title,
      TaskDescription description,
      Priority priority,
      EstimateMinutes estimate,
      LocalDate dueDate,
      TaskStatus status,
      ScheduleSlot schedule,
      long version,
      Instant createdAt,
      Instant updatedAt) {
    return new Task(
        id,
        title,
        description,
        priority,
        estimate,
        dueDate,
        status,
        schedule,
        version,
        createdAt,
        updatedAt);
  }

  /**
   * FR-004, BR-006: Replaces the full content set, keeping status and placement ownership
   * elsewhere.
   *
   * <p>When a new estimate resizes an existing placement the start time is preserved and the end
   * time is recalculated, so an out-of-window result fails here before any persistence work.
   */
  public Task update(
      String newTitle,
      String newDescription,
      Priority newPriority,
      int newEstimateMinutes,
      LocalDate newDueDate,
      Instant now) {
    Objects.requireNonNull(now, "Update time is required");
    if (now.isBefore(updatedAt)) {
      throw new DomainValidationException("TASK_TIME_INVALID", "Update time cannot move backwards");
    }
    EstimateMinutes newEstimate = EstimateMinutes.of(newEstimateMinutes);
    ScheduleSlot resized =
        schedule == null ? null : ScheduleSlot.of(schedule.date(), schedule.start(), newEstimate);
    return new Task(
        id,
        TaskTitle.of(newTitle),
        TaskDescription.of(newDescription),
        Objects.requireNonNull(newPriority, "Task priority is required"),
        newEstimate,
        newDueDate,
        status,
        resized,
        version + 1,
        createdAt,
        now);
  }

  public Task schedule(ScheduleSlot newSchedule, Instant now) {
    Objects.requireNonNull(newSchedule, "Schedule is required");
    if (newSchedule.durationMinutes() != estimate.value()) {
      throw new DomainValidationException(
          "SCHEDULE_DURATION_INVALID", "Schedule duration must match task estimate");
    }
    if (newSchedule.equals(schedule)) {
      return this;
    }
    return copy(status, newSchedule, version + 1, now);
  }

  public Task unschedule(Instant now) {
    return schedule == null ? this : copy(status, null, version + 1, now);
  }

  public Task setCompleted(boolean completed, Instant now) {
    TaskStatus desired = completed ? TaskStatus.COMPLETED : TaskStatus.TODO;
    return desired == status ? this : copy(desired, schedule, version + 1, now);
  }

  private Task copy(TaskStatus newStatus, ScheduleSlot newSchedule, long newVersion, Instant now) {
    Objects.requireNonNull(now, "Update time is required");
    if (now.isBefore(updatedAt)) {
      throw new DomainValidationException("TASK_TIME_INVALID", "Update time cannot move backwards");
    }
    return new Task(
        id,
        title,
        description,
        priority,
        estimate,
        dueDate,
        newStatus,
        newSchedule,
        newVersion,
        createdAt,
        now);
  }

  public TaskId id() {
    return id;
  }

  public TaskTitle title() {
    return title;
  }

  public Optional<TaskDescription> description() {
    return Optional.ofNullable(description);
  }

  public Priority priority() {
    return priority;
  }

  public EstimateMinutes estimate() {
    return estimate;
  }

  public Optional<LocalDate> dueDate() {
    return Optional.ofNullable(dueDate);
  }

  public TaskStatus status() {
    return status;
  }

  public Optional<ScheduleSlot> schedule() {
    return Optional.ofNullable(schedule);
  }

  public long version() {
    return version;
  }

  public Instant createdAt() {
    return createdAt;
  }

  public Instant updatedAt() {
    return updatedAt;
  }
}
