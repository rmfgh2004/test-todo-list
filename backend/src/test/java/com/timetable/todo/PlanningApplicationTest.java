package com.timetable.todo;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PlanningApplicationTest {

  @Test
  void NFR_007_application_entry_point_is_available() {
    assertThat(PlanningApplication.class).isNotNull();
  }
}
