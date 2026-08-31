package com.timetable.todo.planning.application;

import com.timetable.todo.planning.domain.TaskId;

/** FR-008: Requests removal of the placement while preserving task content. */
public record UnscheduleTaskCommand(TaskId taskId, long expectedVersion) {}
