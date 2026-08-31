package com.timetable.todo.planning.adapter.out.persistence;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

/** NFR-001: Registers the persistence inspection helper for adapter contract tests. */
@TestConfiguration(proxyBeanMethods = false)
public class PlanningTestStoreConfiguration {

  @Bean
  PlanningTestStore planningTestStore(TaskJpaRepository tasks, AuditEventJpaRepository audits) {
    return new PlanningTestStore(tasks, audits);
  }
}
