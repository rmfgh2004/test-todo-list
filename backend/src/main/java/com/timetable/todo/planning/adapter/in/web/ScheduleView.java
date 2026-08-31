package com.timetable.todo.planning.adapter.in.web;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDate;
import java.time.LocalTime;

/** FR-006: Transport representation of a half-open placement in Asia/Seoul wall time. */
public record ScheduleView(
    LocalDate date,
    @JsonFormat(pattern = "HH:mm") LocalTime startTime,
    @JsonFormat(pattern = "HH:mm") LocalTime endTime) {}
