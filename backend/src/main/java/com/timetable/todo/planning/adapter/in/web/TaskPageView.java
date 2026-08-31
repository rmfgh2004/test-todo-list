package com.timetable.todo.planning.adapter.in.web;

import java.util.List;

/** FR-010, NFR-005: Bounded task list page with stable metadata. */
public record TaskPageView(
    List<TaskView> content, int page, int size, long totalElements, int totalPages) {}
