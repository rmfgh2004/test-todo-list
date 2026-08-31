package com.timetable.todo.planning.adapter.in.web;

import com.timetable.todo.planning.application.ScheduleOutcome;
import com.timetable.todo.platform.RequestCorrelation;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

/** FR-007, NFR-004: Turns a typed placement outcome into its committed or conflict response. */
final class ScheduleOutcomeResponder {

  private ScheduleOutcomeResponder() {}

  static ResponseEntity<?> respond(ScheduleOutcome outcome) {
    if (outcome instanceof ScheduleOutcome.Committed committed) {
      return ResponseEntity.ok(TaskViewMapper.toView(committed.task()));
    }
    ScheduleOutcome.Conflict conflict = (ScheduleOutcome.Conflict) outcome;
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .contentType(MediaType.APPLICATION_JSON)
        .body(
            ApiError.conflict(
                "The requested time overlaps an existing plan",
                RequestCorrelation.current(),
                new ApiError.ConflictView(
                    TaskViewMapper.toView(conflict.proposed()),
                    TaskViewMapper.toView(conflict.conflicting()),
                    TaskViewMapper.toView(conflict.nextCandidate()))));
  }
}
