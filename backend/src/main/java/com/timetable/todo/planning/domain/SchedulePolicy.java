package com.timetable.todo.planning.domain;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** FR-007, NFR-002: Provides deterministic collision and next-slot policies. */
public final class SchedulePolicy {

  private static final LocalTime DAY_START = LocalTime.of(8, 0);
  private static final LocalTime DAY_END = LocalTime.of(22, 0);

  private SchedulePolicy() {}

  public static boolean overlaps(ScheduleSlot first, ScheduleSlot second) {
    Objects.requireNonNull(first, "First slot is required");
    Objects.requireNonNull(second, "Second slot is required");
    return first.date().equals(second.date())
        && first.start().isBefore(second.end())
        && second.start().isBefore(first.end());
  }

  public static Optional<ScheduleSlot> findNextAvailable(
      WeekRange week,
      LocalDate proposedDate,
      LocalTime proposedStart,
      EstimateMinutes estimate,
      List<ScheduleSlot> occupiedSlots) {
    Objects.requireNonNull(week, "Week range is required");
    Objects.requireNonNull(proposedDate, "Proposed date is required");
    Objects.requireNonNull(proposedStart, "Proposed time is required");
    Objects.requireNonNull(estimate, "Estimate is required");
    List<ScheduleSlot> occupied = List.copyOf(occupiedSlots);

    LocalDate date = proposedDate.isBefore(week.start()) ? week.start() : proposedDate;
    if (!week.contains(date)) {
      return Optional.empty();
    }

    while (week.contains(date)) {
      LocalTime candidateStart =
          date.equals(proposedDate) ? ceilToQuarter(proposedStart) : DAY_START;
      if (candidateStart.isBefore(DAY_START)) {
        candidateStart = DAY_START;
      }
      while (candidateStart.isBefore(DAY_END)) {
        LocalTime candidateEnd = candidateStart.plusMinutes(estimate.value());
        if (candidateEnd.isAfter(candidateStart) && !candidateEnd.isAfter(DAY_END)) {
          ScheduleSlot candidate = new ScheduleSlot(date, candidateStart, candidateEnd);
          if (occupied.stream().noneMatch(slot -> overlaps(candidate, slot))) {
            return Optional.of(candidate);
          }
        }
        candidateStart = candidateStart.plusMinutes(15);
      }
      date = date.plusDays(1);
    }
    return Optional.empty();
  }

  private static LocalTime ceilToQuarter(LocalTime time) {
    int minuteOfDay = time.getHour() * 60 + time.getMinute();
    if (time.getSecond() > 0 || time.getNano() > 0) {
      minuteOfDay++;
    }
    int rounded = ((minuteOfDay + 14) / 15) * 15;
    return rounded >= 24 * 60 ? DAY_END : LocalTime.of(rounded / 60, rounded % 60);
  }
}
