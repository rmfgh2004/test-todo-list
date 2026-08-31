package com.timetable.todo.planning.adapter.in.web;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

/** FR-009: Desired final completion state with the expected optimistic version. */
public record SetCompletionRequest(
    @NotNull Boolean completed, @NotNull @PositiveOrZero Long expectedVersion) {}
