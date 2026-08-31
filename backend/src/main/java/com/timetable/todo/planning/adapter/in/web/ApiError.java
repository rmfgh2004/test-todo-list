package com.timetable.todo.planning.adapter.in.web;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/**
 * NFR-003, NFR-004, NFR-008: The single safe error representation of the planning API.
 *
 * <p>Only an allowlisted code, an authored message, the correlation ID and allowlisted field names
 * are serialized. Rejected values, task content, SQL, paths and exception text are never included.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(
    String code,
    String message,
    String requestId,
    List<FieldError> fieldErrors,
    Long currentVersion,
    ConflictView conflict) {

  public static ApiError of(String code, String message, String requestId) {
    return new ApiError(code, message, requestId, null, null, null);
  }

  public static ApiError validation(
      String message, String requestId, List<FieldError> fieldErrors) {
    return new ApiError("VALIDATION_FAILED", message, requestId, fieldErrors, null, null);
  }

  public static ApiError stale(String message, String requestId, Long currentVersion) {
    return new ApiError("STALE_TASK", message, requestId, null, currentVersion, null);
  }

  public static ApiError conflict(String message, String requestId, ConflictView conflict) {
    return new ApiError("SCHEDULE_CONFLICT", message, requestId, null, null, conflict);
  }

  /** NFR-004: Names the rejected field and its stable code without echoing the rejected value. */
  public record FieldError(String field, String code, String message) {}

  /** FR-007: Recovery metadata for a rejected placement; it carries times, never task content. */
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record ConflictView(
      ScheduleView proposed, ScheduleView conflicting, ScheduleView nextCandidate) {}
}
