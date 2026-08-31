package com.timetable.todo.planning.application;

/** FR-010: Allowlisted fields for task list ordering. */
public enum TaskSort {
  CREATED_AT("createdAt"),
  DUE_DATE("dueDate"),
  PRIORITY("priority"),
  TITLE("title");

  private final String property;

  TaskSort(String property) {
    this.property = property;
  }

  public String property() {
    return property;
  }
}
