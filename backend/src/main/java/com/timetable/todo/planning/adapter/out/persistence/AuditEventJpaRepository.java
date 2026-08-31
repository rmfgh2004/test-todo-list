package com.timetable.todo.planning.adapter.out.persistence;

import java.util.UUID;
import org.springframework.data.repository.ListCrudRepository;

interface AuditEventJpaRepository extends ListCrudRepository<AuditEventJpaEntity, UUID> {}
