package com.timetable.todo.planning;

import com.timetable.todo.planning.application.AuditPort;
import com.timetable.todo.planning.application.PlanningService;
import com.timetable.todo.planning.application.TaskRepositoryPort;
import java.time.Clock;
import java.util.UUID;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** NFR-007: Wires the framework-free application service to its outbound adapters. */
@Configuration(proxyBeanMethods = false)
public class PlanningConfiguration {

  @Bean
  Clock planningClock() {
    return Clock.systemUTC();
  }

  @Bean
  PlanningService planningService(TaskRepositoryPort tasks, AuditPort audits, Clock clock) {
    return new PlanningService(tasks, audits, clock, UUID::randomUUID);
  }
}
