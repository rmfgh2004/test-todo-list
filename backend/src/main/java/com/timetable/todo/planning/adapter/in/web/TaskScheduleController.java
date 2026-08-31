package com.timetable.todo.planning.adapter.in.web;

import com.timetable.todo.planning.application.PlanningService;
import com.timetable.todo.planning.application.ScheduleOutcome;
import com.timetable.todo.planning.application.ScheduleTaskCommand;
import com.timetable.todo.planning.application.SetCompletionCommand;
import com.timetable.todo.planning.application.UnscheduleTaskCommand;
import com.timetable.todo.planning.domain.TaskId;
import com.timetable.todo.platform.RequestCorrelation;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** FR-006, FR-007, FR-008, FR-009: Placement and completion endpoints of the planning API. */
@RestController
@RequestMapping("/api/v1/tasks/{id}")
class TaskScheduleController {

  private final PlanningService planning;

  TaskScheduleController(PlanningService planning) {
    this.planning = planning;
  }

  /** FR-006, FR-007: Places or moves a task; an overlap returns a conflict and writes nothing. */
  @PutMapping("/schedule")
  ResponseEntity<?> schedule(
      @PathVariable UUID id, @Valid @RequestBody ScheduleTaskRequest request) {
    ScheduleOutcome outcome =
        planning.schedule(
            new ScheduleTaskCommand(
                new TaskId(id), request.date(), request.startTime(), request.expectedVersion()),
            RequestCorrelation.current());
    return ScheduleOutcomeResponder.respond(outcome);
  }

  /** FR-008: Removes the placement only and is idempotent for an unscheduled task. */
  @DeleteMapping("/schedule")
  TaskView unschedule(@PathVariable UUID id, @RequestParam long expectedVersion) {
    return TaskViewMapper.toView(
        planning.unschedule(
            new UnscheduleTaskCommand(new TaskId(id), expectedVersion),
            RequestCorrelation.current()));
  }

  /** FR-009: Applies a desired final completion state idempotently. */
  @PutMapping("/completion")
  TaskView setCompletion(@PathVariable UUID id, @Valid @RequestBody SetCompletionRequest request) {
    return TaskViewMapper.toView(
        planning.setCompletion(
            new SetCompletionCommand(
                new TaskId(id), request.completed(), request.expectedVersion()),
            RequestCorrelation.current()));
  }
}
