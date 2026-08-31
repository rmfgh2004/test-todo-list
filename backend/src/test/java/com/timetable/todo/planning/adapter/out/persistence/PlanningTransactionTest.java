package com.timetable.todo.planning.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.timetable.todo.planning.application.AuditPort;
import com.timetable.todo.planning.application.CreateTaskCommand;
import com.timetable.todo.planning.application.PlanningService;
import com.timetable.todo.planning.domain.Priority;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@DataJpaTest
@Import({TaskPersistenceAdapter.class, PlanningTransactionTest.Configuration.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class PlanningTransactionTest {

  private static final UUID TASK_ID = UUID.fromString("c1a9bb1e-d1f9-4465-afdf-b865fab7f92d");

  @Autowired private PlanningService service;
  @Autowired private TaskJpaRepository repository;

  @Test
  void FR_013_task_write_rolls_back_when_audit_append_fails() {
    assertThatThrownBy(
            () ->
                service.create(
                    new CreateTaskCommand("rollback", null, Priority.MEDIUM, 30, null),
                    "request-rollback"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("simulated audit failure");

    assertThat(repository.findById(TASK_ID)).isEmpty();
  }

  @TestConfiguration(proxyBeanMethods = false)
  static class Configuration {

    @Bean
    PlanningService planningService(TaskPersistenceAdapter tasks, AuditPort audits) {
      return new PlanningService(
          tasks,
          audits,
          Clock.fixed(Instant.parse("2026-08-31T09:00:00Z"), ZoneOffset.UTC),
          () -> TASK_ID);
    }

    @Bean
    AuditPort failingAuditPort() {
      return ignored -> {
        throw new IllegalStateException("simulated audit failure");
      };
    }
  }
}
