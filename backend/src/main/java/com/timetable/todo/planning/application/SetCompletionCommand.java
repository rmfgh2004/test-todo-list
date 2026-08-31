package com.timetable.todo.planning.application;

import com.timetable.todo.planning.domain.TaskId;

/** FR-009: Requests an explicit desired completion state. */
public record SetCompletionCommand(TaskId taskId, boolean completed, long expectedVersion) {}
