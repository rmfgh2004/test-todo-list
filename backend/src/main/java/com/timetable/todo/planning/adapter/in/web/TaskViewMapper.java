package com.timetable.todo.planning.adapter.in.web;

import com.timetable.todo.planning.application.WeeklyPlan;
import com.timetable.todo.planning.domain.ScheduleSlot;
import com.timetable.todo.planning.domain.Task;
import com.timetable.todo.planning.domain.TaskDescription;
import java.util.List;
import java.util.Optional;

/** NFR-003, NFR-007: Maps domain aggregates to transport views and nothing else. */
final class TaskViewMapper {

  private TaskViewMapper() {}

  static TaskView toView(Task task) {
    return new TaskView(
        task.id().value(),
        task.title().value(),
        task.description().map(TaskDescription::value).orElse(null),
        task.priority(),
        task.estimate().value(),
        task.dueDate().orElse(null),
        task.status(),
        task.schedule().map(TaskViewMapper::toView).orElse(null),
        task.version(),
        task.createdAt(),
        task.updatedAt());
  }

  static ScheduleView toView(ScheduleSlot slot) {
    return new ScheduleView(slot.date(), slot.start(), slot.end());
  }

  static ScheduleView toView(Optional<ScheduleSlot> slot) {
    return slot.map(TaskViewMapper::toView).orElse(null);
  }

  static List<TaskView> toViews(List<Task> tasks) {
    return tasks.stream().map(TaskViewMapper::toView).toList();
  }

  static WeeklyPlanView toView(WeeklyPlan plan) {
    return new WeeklyPlanView(
        plan.week().start(),
        plan.week().endExclusive(),
        toViews(plan.scheduled()),
        toViews(plan.backlog()));
  }
}
