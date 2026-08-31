package com.timetable.todo.planning.adapter.in.web;

import java.time.LocalDate;
import java.util.List;

/** FR-001, FR-005: Bounded weekly placement view with the unscheduled backlog. */
public record WeeklyPlanView(
    LocalDate weekStart,
    LocalDate weekEndExclusive,
    List<TaskView> scheduled,
    List<TaskView> backlog) {}
