package com.timetable.todo.planning.adapter.out.persistence;

import com.timetable.todo.planning.application.SortDirection;
import com.timetable.todo.planning.application.StaleTaskVersionException;
import com.timetable.todo.planning.application.TaskListQuery;
import com.timetable.todo.planning.application.TaskPage;
import com.timetable.todo.planning.application.TaskRepositoryPort;
import com.timetable.todo.planning.domain.EstimateMinutes;
import com.timetable.todo.planning.domain.ScheduleSlot;
import com.timetable.todo.planning.domain.Task;
import com.timetable.todo.planning.domain.TaskDescription;
import com.timetable.todo.planning.domain.TaskId;
import com.timetable.todo.planning.domain.TaskTitle;
import com.timetable.todo.planning.domain.WeekRange;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

/** FR-012, NFR-006: Persists Task aggregates with bound JPA queries and optimistic locking. */
@Repository
public class TaskPersistenceAdapter implements TaskRepositoryPort {

  private final TaskJpaRepository repository;

  TaskPersistenceAdapter(TaskJpaRepository repository) {
    this.repository = repository;
  }

  @Override
  public Optional<Task> findById(TaskId id) {
    return repository.findWithScheduleById(id.value()).map(this::toDomain);
  }

  @Override
  public Task save(Task task) {
    TaskJpaEntity entity = repository.findWithScheduleById(task.id().value()).orElse(null);
    if (entity == null) {
      entity =
          new TaskJpaEntity(
              task.id().value(),
              task.title().value(),
              task.description().map(TaskDescription::value).orElse(null),
              task.priority(),
              task.estimate().value(),
              task.dueDate().orElse(null),
              task.status(),
              task.createdAt(),
              task.updatedAt());
    } else if (task.version() != entity.getVersion() + 1) {
      throw new StaleTaskVersionException();
    }
    entity.updateFrom(task);
    return toDomain(repository.saveAndFlush(entity));
  }

  @Override
  public void delete(TaskId id) {
    repository.deleteById(id.value());
    repository.flush();
  }

  @Override
  public List<Task> findScheduledIncomplete(WeekRange week) {
    return repository.findScheduledIncomplete(week.start(), week.endExclusive()).stream()
        .map(this::toDomain)
        .toList();
  }

  @Override
  public List<Task> findScheduledInWeek(WeekRange week) {
    return repository.findScheduledInWeek(week.start(), week.endExclusive()).stream()
        .map(this::toDomain)
        .toList();
  }

  @Override
  public List<Task> findBacklog(int limit) {
    return repository.findBacklog(PageRequest.of(0, limit)).stream().map(this::toDomain).toList();
  }

  @Override
  public TaskPage findPage(TaskListQuery query) {
    Specification<TaskJpaEntity> specification = Specification.unrestricted();
    if (query.status() != null) {
      specification =
          specification.and(
              (root, ignored, builder) -> builder.equal(root.get("status"), query.status()));
    }
    if (query.priority() != null) {
      specification =
          specification.and(
              (root, ignored, builder) -> builder.equal(root.get("priority"), query.priority()));
    }
    if (query.scheduled() != null) {
      specification =
          specification.and(
              (root, ignored, builder) -> {
                var schedule = root.join("schedule", jakarta.persistence.criteria.JoinType.LEFT);
                return query.scheduled()
                    ? builder.isNotNull(schedule.get("task"))
                    : builder.isNull(schedule.get("task"));
              });
    }
    Sort.Direction direction =
        query.direction() == SortDirection.ASC ? Sort.Direction.ASC : Sort.Direction.DESC;
    Sort sort = Sort.by(direction, query.sort().property()).and(Sort.by("id"));
    org.springframework.data.domain.Page<TaskJpaEntity> result =
        repository.findAll(specification, PageRequest.of(query.page(), query.size(), sort));
    return new TaskPage(
        result.getContent().stream().map(this::toDomain).toList(),
        result.getNumber(),
        result.getSize(),
        result.getTotalElements(),
        result.getTotalPages());
  }

  private Task toDomain(TaskJpaEntity entity) {
    ScheduleSlot schedule =
        Optional.ofNullable(entity.getSchedule())
            .map(slot -> new ScheduleSlot(slot.getDate(), slot.getStart(), slot.getEnd()))
            .orElse(null);
    return Task.rehydrate(
        new TaskId(entity.getId()),
        TaskTitle.of(entity.getTitle()),
        TaskDescription.of(entity.getDescription()),
        entity.getPriority(),
        EstimateMinutes.of(entity.getEstimateMinutes()),
        entity.getDueDate(),
        entity.getStatus(),
        schedule,
        entity.getVersion(),
        entity.getCreatedAt(),
        entity.getUpdatedAt());
  }
}
