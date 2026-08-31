package com.timetable.todo.planning.domain;

/** FR-003: Enforces the task duration and 15-minute grid invariant. */
public record EstimateMinutes(int value) {

  private static final int MINIMUM = 15;
  private static final int MAXIMUM = 840;

  public static EstimateMinutes of(int value) {
    if (value < MINIMUM || value > MAXIMUM || value % 15 != 0) {
      throw new DomainValidationException("TASK_ESTIMATE_INVALID", "Task estimate is invalid");
    }
    return new EstimateMinutes(value);
  }
}
