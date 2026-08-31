package com.timetable.todo.planning.application;

import com.timetable.todo.planning.domain.Priority;
import com.timetable.todo.planning.domain.TaskId;
import java.time.LocalDate;

/** FR-004: Carries a validated full content replacement into the application boundary. */
public record UpdateTaskCommand(
    TaskId taskId,
    String title,
    String description,
    Priority priority,
    int estimateMinutes,
    LocalDate dueDate,
    long expectedVersion) {}
