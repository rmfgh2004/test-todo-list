package com.timetable.todo.planning.application;

import com.timetable.todo.planning.domain.TaskId;

/** FR-004: Requests confirmed task deletion with optimistic version. */
public record DeleteTaskCommand(TaskId taskId, long expectedVersion, boolean confirmed) {}
