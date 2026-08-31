package com.timetable.todo.planning.adapter.in.web;

import com.timetable.todo.planning.domain.Priority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * FR-004, BR-006: Full content replacement payload.
 *
 * <p>Title, priority and estimate are mandatory; an omitted description or due date clears that
 * value. Completion state and placement are owned by their dedicated endpoints.
 */
public record UpdateTaskRequest(
    @NotNull @PositiveOrZero Long expectedVersion,
    @NotBlank @Size(max = 120) String title,
    @Size(max = 2000) String description,
    @NotNull Priority priority,
    @NotNull @QuarterHourEstimate Integer estimateMinutes,
    LocalDate dueDate) {}
