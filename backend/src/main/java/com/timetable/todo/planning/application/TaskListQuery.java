package com.timetable.todo.planning.application;

import com.timetable.todo.planning.domain.DomainValidationException;
import com.timetable.todo.planning.domain.Priority;
import com.timetable.todo.planning.domain.TaskStatus;
import java.util.Objects;

/** FR-010, NFR-005: A typed and bounded task list query. */
public record TaskListQuery(
    TaskStatus status,
    Boolean scheduled,
    Priority priority,
    TaskSort sort,
    SortDirection direction,
    int page,
    int size) {

  public TaskListQuery {
    Objects.requireNonNull(sort, "Task sort is required");
    Objects.requireNonNull(direction, "Sort direction is required");
    if (page < 0) {
      throw new DomainValidationException("PAGE_INVALID", "Page must not be negative");
    }
    if (size < 1 || size > 100) {
      throw new DomainValidationException(
          "PAGE_SIZE_INVALID", "Page size must be between 1 and 100");
    }
  }
}
