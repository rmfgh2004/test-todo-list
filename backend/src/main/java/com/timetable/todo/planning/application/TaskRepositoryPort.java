package com.timetable.todo.planning.application;

import com.timetable.todo.planning.domain.Task;
import com.timetable.todo.planning.domain.TaskId;
import com.timetable.todo.planning.domain.WeekRange;
import java.util.List;
import java.util.Optional;

/** NFR-007: Keeps application use cases independent from JPA. */
public interface TaskRepositoryPort {

  Optional<Task> findById(TaskId id);

  Task save(Task task);

  void delete(TaskId id);

  List<Task> findScheduledIncomplete(WeekRange week);

  List<Task> findBacklog(int limit);

  TaskPage findPage(TaskListQuery query);
}
