package com.timetable.todo.planning.application;

import com.timetable.todo.planning.domain.Task;
import com.timetable.todo.planning.domain.WeekRange;
import java.util.List;

/** FR-001, FR-005: Bounded weekly placement view with the current unscheduled backlog. */
public record WeeklyPlan(WeekRange week, List<Task> scheduled, List<Task> backlog) {

  public WeeklyPlan {
    scheduled = List.copyOf(scheduled);
    backlog = List.copyOf(backlog);
  }
}
