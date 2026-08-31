package com.timetable.todo.planning.application;

import com.timetable.todo.planning.domain.Priority;
import java.time.LocalDate;

/** FR-003: Carries validated create intent into the application boundary. */
public record CreateTaskCommand(
    String title, String description, Priority priority, int estimateMinutes, LocalDate dueDate) {}
