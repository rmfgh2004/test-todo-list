package com.timetable.todo.planning.adapter.in.web;

import com.timetable.todo.planning.domain.Priority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * FR-003, SECURITY-05: Allowlisted create payload for task content.
 *
 * <p>Every field has an explicit type, size and format bound before any repository work runs.
 */
public record TaskContentRequest(
    @NotBlank @Size(max = 120) String title,
    @Size(max = 2000) String description,
    @NotNull Priority priority,
    @NotNull @QuarterHourEstimate Integer estimateMinutes,
    LocalDate dueDate) {}
