package com.timetable.todo.planning.adapter.out.persistence;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface TaskJpaRepository
    extends JpaRepository<TaskJpaEntity, UUID>, JpaSpecificationExecutor<TaskJpaEntity> {

  @Override
  @EntityGraph(attributePaths = "schedule")
  Page<TaskJpaEntity> findAll(Specification<TaskJpaEntity> specification, Pageable pageable);

  @EntityGraph(attributePaths = "schedule")
  @Query("select t from TaskJpaEntity t where t.id = :id")
  java.util.Optional<TaskJpaEntity> findWithScheduleById(@Param("id") UUID id);

  @EntityGraph(attributePaths = "schedule")
  @Query(
      "select t from TaskJpaEntity t join t.schedule s "
          + "where t.status = 'TODO' and s.date >= :start and s.date < :end "
          + "order by s.date, s.start, t.id")
  List<TaskJpaEntity> findScheduledIncomplete(
      @Param("start") LocalDate start, @Param("end") LocalDate end);

  @EntityGraph(attributePaths = "schedule")
  @Query(
      "select t from TaskJpaEntity t left join t.schedule s "
          + "where t.status = 'TODO' and s is null "
          + "order by case when t.dueDate is null then 1 else 0 end, t.dueDate, "
          + "case t.priority when 'HIGH' then 0 when 'MEDIUM' then 1 else 2 end, "
          + "t.createdAt, t.id")
  List<TaskJpaEntity> findBacklog(Pageable pageable);
}
