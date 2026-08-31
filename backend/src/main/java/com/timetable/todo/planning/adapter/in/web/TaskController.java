package com.timetable.todo.planning.adapter.in.web;

import com.timetable.todo.planning.application.CreateTaskCommand;
import com.timetable.todo.planning.application.DeleteTaskCommand;
import com.timetable.todo.planning.application.PlanningService;
import com.timetable.todo.planning.application.ScheduleOutcome;
import com.timetable.todo.planning.application.SortDirection;
import com.timetable.todo.planning.application.TaskListQuery;
import com.timetable.todo.planning.application.TaskPage;
import com.timetable.todo.planning.application.TaskSort;
import com.timetable.todo.planning.application.UpdateTaskCommand;
import com.timetable.todo.planning.domain.Priority;
import com.timetable.todo.planning.domain.Task;
import com.timetable.todo.planning.domain.TaskId;
import com.timetable.todo.planning.domain.TaskStatus;
import com.timetable.todo.platform.RequestCorrelation;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** FR-003, FR-004, FR-010: Task content and list endpoints of the planning API. */
@RestController
@RequestMapping("/api/v1/tasks")
class TaskController {

  private final PlanningService planning;

  TaskController(PlanningService planning) {
    this.planning = planning;
  }

  /** FR-003: Creates a task and returns its backlog view. */
  @PostMapping
  ResponseEntity<TaskView> create(@Valid @RequestBody TaskContentRequest request) {
    Task created =
        planning.create(
            new CreateTaskCommand(
                request.title(),
                request.description(),
                request.priority(),
                request.estimateMinutes(),
                request.dueDate()),
            RequestCorrelation.current());
    TaskView view = TaskViewMapper.toView(created);
    return ResponseEntity.created(URI.create("/api/v1/tasks/" + view.id())).body(view);
  }

  /** FR-004: Returns one task detail or a safe not-found error. */
  @GetMapping("/{id}")
  TaskView get(@PathVariable UUID id) {
    return TaskViewMapper.toView(planning.findById(new TaskId(id)));
  }

  /**
   * FR-004, FR-007: Replaces the full content set.
   *
   * <p>A changed estimate resizes an existing placement in place, so the response can be a conflict
   * that leaves the stored task untouched.
   */
  @PatchMapping("/{id}")
  ResponseEntity<?> update(@PathVariable UUID id, @Valid @RequestBody UpdateTaskRequest request) {
    ScheduleOutcome outcome =
        planning.update(
            new UpdateTaskCommand(
                new TaskId(id),
                request.title(),
                request.description(),
                request.priority(),
                request.estimateMinutes(),
                request.dueDate(),
                request.expectedVersion()),
            RequestCorrelation.current());
    return ScheduleOutcomeResponder.respond(outcome);
  }

  /** FR-004, BR-024: Deletes a task only after an explicit confirmation and version match. */
  @DeleteMapping("/{id}")
  ResponseEntity<Void> delete(
      @PathVariable UUID id, @RequestParam long expectedVersion, @RequestParam boolean confirmed) {
    planning.delete(
        new DeleteTaskCommand(new TaskId(id), expectedVersion, confirmed),
        RequestCorrelation.current());
    return ResponseEntity.noContent().build();
  }

  /** FR-010, NFR-005: Returns a bounded, allowlist-filtered task page. */
  @GetMapping
  TaskPageView list(
      @RequestParam(required = false) TaskStatus status,
      @RequestParam(required = false) Boolean scheduled,
      @RequestParam(required = false) Priority priority,
      @RequestParam(defaultValue = "CREATED_AT") TaskSort sort,
      @RequestParam(defaultValue = "DESC") SortDirection direction,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "25") int size) {
    TaskPage result =
        planning.list(new TaskListQuery(status, scheduled, priority, sort, direction, page, size));
    return new TaskPageView(
        TaskViewMapper.toViews(result.content()),
        result.page(),
        result.size(),
        result.totalElements(),
        result.totalPages());
  }
}
