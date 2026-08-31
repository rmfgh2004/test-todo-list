package com.timetable.todo.planning.adapter.in.web;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * FR-006, FR-007: Placement request carrying only the start of the interval.
 *
 * <p>The end time is always derived from the persisted estimate, so a client-supplied end is not
 * accepted (BR-013). {@code resolutionMode} is informational: accepting a candidate re-runs the
 * full conflict evaluation against current data exactly like a first placement.
 */
public record ScheduleTaskRequest(
    @NotNull LocalDate date,
    @NotNull @JsonFormat(pattern = "HH:mm") LocalTime startTime,
    @NotNull @PositiveOrZero Long expectedVersion,
    ScheduleResolutionMode resolutionMode) {}
