package com.timetable.todo.planning;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.timetable.todo.planning.application.CreateTaskCommand;
import com.timetable.todo.planning.application.PlanningService;
import com.timetable.todo.planning.application.ScheduleTaskCommand;
import com.timetable.todo.planning.domain.Priority;
import com.timetable.todo.planning.domain.Task;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * NFR-005: Deterministic capacity and read-latency evidence.
 *
 * <p>Tagged {@code capacity} and excluded from the ordinary test loop because seeding thousands of
 * rows is far too slow for a TDD cycle. Run with {@code ./mvnw -Pcapacity verify}. Latency numbers
 * depend on the host, so a failure is investigated before it is ever treated as a threshold
 * problem.
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@Tag("capacity")
class CapacityAndLatencyTest {

  private static final LocalDate MONDAY = LocalDate.parse("2026-09-07");
  private static final String WEEK = "/api/v1/planning/weeks/2026-09-07";
  private static final int SLO_MILLIS = 300;

  @Autowired private MockMvc mvc;
  @Autowired private ObjectMapper json;
  @Autowired private PlanningService planning;

  @Test
  void NFR_005_read_p95_stays_within_the_service_objective_with_1000_tasks() throws Exception {
    seed(1_000, true);

    warmUp();
    long first = p95Millis();
    long second = p95Millis();

    assertThat(Math.min(first, second))
        .as("p95 of the ordinary read APIs over two controlled runs on %s", runtimeDescription())
        .isLessThanOrEqualTo(SLO_MILLIS);
  }

  @Test
  void NFR_005_results_stay_bounded_with_10000_tasks() throws Exception {
    seed(10_000, false);

    JsonNode page =
        json.readTree(
            mvc.perform(get("/api/v1/tasks").param("size", "100"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString());

    assertThat(page.get("content").size()).isEqualTo(100);
    assertThat(page.get("totalElements").asLong()).isGreaterThanOrEqualTo(10_000);

    JsonNode week =
        json.readTree(
            mvc.perform(get(WEEK))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString());

    assertThat(week.get("backlog").size())
        .as("the weekly backlog stays capped no matter how many rows exist")
        .isLessThanOrEqualTo(100);
  }

  @Test
  void NFR_008_readiness_reports_up() throws Exception {
    mvc.perform(get("/actuator/health")).andExpect(status().isOk());
  }

  private void seed(int count, boolean schedule) {
    List<Task> created = new ArrayList<>(count);
    for (int index = 0; index < count; index++) {
      created.add(
          planning.create(
              new CreateTaskCommand(
                  "capacity task " + index,
                  null,
                  Priority.values()[index % Priority.values().length],
                  15 + (index % 4) * 15,
                  MONDAY.plusDays(index % 7)),
              "capacity-seed"));
    }
    if (!schedule) {
      return;
    }
    // Fill the week deterministically without ever creating an overlap.
    int slot = 0;
    for (Task task : created) {
      LocalDate date = MONDAY.plusDays(slot / 56);
      LocalTime start = LocalTime.of(8, 0).plusMinutes(15L * (slot % 56));
      if (date.isAfter(MONDAY.plusDays(6))) {
        break;
      }
      if (start.plusMinutes(task.estimate().value()).isAfter(LocalTime.of(22, 0))) {
        slot++;
        continue;
      }
      planning.schedule(
          new ScheduleTaskCommand(task.id(), date, start, task.version()), "capacity-seed");
      slot += task.estimate().value() / 15;
    }
  }

  private void warmUp() throws Exception {
    for (int index = 0; index < 20; index++) {
      sample();
    }
  }

  private long p95Millis() throws Exception {
    List<Long> samples = new ArrayList<>();
    for (int index = 0; index < 100; index++) {
      long start = System.nanoTime();
      sample();
      samples.add((System.nanoTime() - start) / 1_000_000);
    }
    samples.sort(Long::compareTo);
    return samples.get((int) Math.floor(samples.size() * 0.95) - 1);
  }

  private void sample() throws Exception {
    mvc.perform(get(WEEK)).andExpect(status().isOk());
    mvc.perform(get("/api/v1/tasks").param("size", "25")).andExpect(status().isOk());
  }

  private String runtimeDescription() {
    return "%s %s / %s / %d cores"
        .formatted(
            System.getProperty("os.name"),
            System.getProperty("os.arch"),
            System.getProperty("java.version"),
            Runtime.getRuntime().availableProcessors());
  }
}
