package com.timetable.todo.planning.adapter.in.web;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.timetable.todo.planning.domain.Priority;
import com.timetable.todo.planning.domain.TaskStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * FR-004, NFR-003: Dedicated response representation of a Task.
 *
 * <p>No JPA entity, domain aggregate or exception type ever reaches the transport boundary.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record TaskView(
    UUID id,
    String title,
    String description,
    Priority priority,
    int estimateMinutes,
    LocalDate dueDate,
    TaskStatus status,
    ScheduleView schedule,
    long version,
    Instant createdAt,
    Instant updatedAt) {}
