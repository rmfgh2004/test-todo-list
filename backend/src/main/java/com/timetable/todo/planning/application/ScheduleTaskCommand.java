package com.timetable.todo.planning.application;

import com.timetable.todo.planning.domain.TaskId;
import java.time.LocalDate;
import java.time.LocalTime;

/** FR-006, FR-007: Requests an authoritative schedule decision. */
public record ScheduleTaskCommand(
    TaskId taskId, LocalDate date, LocalTime startTime, long expectedVersion) {}
