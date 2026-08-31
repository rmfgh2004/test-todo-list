package com.timetable.todo.planning.adapter.in.web;

import com.timetable.todo.planning.application.PlanningService;
import com.timetable.todo.planning.domain.WeekRange;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** FR-001, FR-002, FR-005, NFR-005: Bounded weekly plan endpoint. */
@RestController
@RequestMapping("/api/v1/planning/weeks")
class WeeklyPlanController {

  private final PlanningService planning;

  WeeklyPlanController(PlanningService planning) {
    this.planning = planning;
  }

  /**
   * FR-001, BR-030: Returns one Monday-anchored week; a non-Monday start is rejected, not fixed.
   */
  @GetMapping("/{weekStart}")
  WeeklyPlanView week(
      @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart) {
    return TaskViewMapper.toView(planning.weekPlan(WeekRange.fromMonday(weekStart)));
  }
}
