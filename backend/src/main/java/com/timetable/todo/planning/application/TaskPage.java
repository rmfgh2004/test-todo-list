package com.timetable.todo.planning.application;

import com.timetable.todo.planning.domain.Task;
import java.util.List;

/** FR-010: Bounded task list result with stable page metadata. */
public record TaskPage(List<Task> content, int page, int size, long totalElements, int totalPages) {

  public TaskPage {
    content = List.copyOf(content);
  }
}
