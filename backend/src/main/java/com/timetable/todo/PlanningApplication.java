package com.timetable.todo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** NFR-007: Starts the local planning API. */
@SpringBootApplication
public class PlanningApplication {

  public static void main(String[] args) {
    SpringApplication.run(PlanningApplication.class, args);
  }
}
