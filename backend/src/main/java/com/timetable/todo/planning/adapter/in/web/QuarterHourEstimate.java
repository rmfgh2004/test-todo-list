package com.timetable.todo.planning.adapter.in.web;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** FR-003, BR-004, SECURITY-05: Allowlists the 15~840 minute estimate on the 15-minute grid. */
@Documented
@Constraint(validatedBy = QuarterHourEstimateValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface QuarterHourEstimate {

  String message() default "must be 15 to 840 minutes in 15 minute steps";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
}
