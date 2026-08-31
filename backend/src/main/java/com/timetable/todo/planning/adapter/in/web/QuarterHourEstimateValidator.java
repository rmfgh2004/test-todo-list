package com.timetable.todo.planning.adapter.in.web;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/** FR-003, BR-004: Rejects an estimate outside the allowlisted range or off the 15-minute grid. */
public class QuarterHourEstimateValidator
    implements ConstraintValidator<QuarterHourEstimate, Integer> {

  private static final int MINIMUM = 15;
  private static final int MAXIMUM = 840;

  @Override
  public boolean isValid(Integer value, ConstraintValidatorContext context) {
    return value == null || (value >= MINIMUM && value <= MAXIMUM && value % 15 == 0);
  }
}
