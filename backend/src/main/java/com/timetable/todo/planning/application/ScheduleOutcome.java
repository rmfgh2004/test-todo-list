package com.timetable.todo.planning.application;

import com.timetable.todo.planning.domain.ScheduleSlot;
import com.timetable.todo.planning.domain.Task;
import java.util.Optional;

/** FR-007: Represents either a committed task state or a non-mutating placement conflict. */
public sealed interface ScheduleOutcome {

  record Committed(Task task) implements ScheduleOutcome {}

  record Conflict(
      ScheduleSlot proposed, ScheduleSlot conflicting, Optional<ScheduleSlot> nextCandidate)
      implements ScheduleOutcome {

    public Conflict {
      nextCandidate = nextCandidate == null ? Optional.empty() : nextCandidate;
    }
  }
}
