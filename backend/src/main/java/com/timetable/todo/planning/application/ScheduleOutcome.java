package com.timetable.todo.planning.application;

import com.timetable.todo.planning.domain.ScheduleSlot;
import com.timetable.todo.planning.domain.Task;
import java.util.Optional;

/** FR-007: Represents either a committed schedule or a non-mutating conflict. */
public sealed interface ScheduleOutcome {

  record Scheduled(Task task) implements ScheduleOutcome {}

  record Conflict(
      ScheduleSlot proposed, ScheduleSlot conflicting, Optional<ScheduleSlot> nextCandidate)
      implements ScheduleOutcome {

    public Conflict {
      nextCandidate = nextCandidate == null ? Optional.empty() : nextCandidate;
    }
  }
}
