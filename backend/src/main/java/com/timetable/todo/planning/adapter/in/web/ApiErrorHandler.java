package com.timetable.todo.planning.adapter.in.web;

import com.timetable.todo.planning.application.DeletionNotConfirmedException;
import com.timetable.todo.planning.application.StaleTaskVersionException;
import com.timetable.todo.planning.application.TaskNotFoundException;
import com.timetable.todo.planning.domain.DomainValidationException;
import com.timetable.todo.platform.RequestCorrelation;
import java.util.Comparator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * NFR-003, NFR-004, NFR-008, SECURITY-03: The single safe error boundary of the planning API.
 *
 * <p>Every response is built from an authored message catalog. A rejected value, task content, SQL
 * fragment, stack trace, framework type name or filesystem path is never serialized to a client.
 */
@RestControllerAdvice
class ApiErrorHandler {

  private static final Logger log = LoggerFactory.getLogger(ApiErrorHandler.class);

  /** FR-004: A missing task never reveals whether the identifier ever existed. */
  @ExceptionHandler(TaskNotFoundException.class)
  ResponseEntity<ApiError> handleTaskNotFound(TaskNotFoundException ignored) {
    return error(HttpStatus.NOT_FOUND, "TASK_NOT_FOUND", "The requested task was not found");
  }

  /** NFR-006: A stale command is rejected with the version the client must reload. */
  @ExceptionHandler(StaleTaskVersionException.class)
  ResponseEntity<ApiError> handleStaleVersion(StaleTaskVersionException exception) {
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .contentType(MediaType.APPLICATION_JSON)
        .body(
            ApiError.stale(
                "The task changed since it was loaded; reload and try again",
                RequestCorrelation.current(),
                exception.currentVersion()));
  }

  /** BR-024: A destructive command requires an explicit confirmation contract. */
  @ExceptionHandler(DeletionNotConfirmedException.class)
  ResponseEntity<ApiError> handleUnconfirmedDeletion(DeletionNotConfirmedException ignored) {
    return error(
        HttpStatus.BAD_REQUEST,
        "DELETION_NOT_CONFIRMED",
        "Deleting a task requires an explicit confirmation");
  }

  /** SECURITY-05: A domain rule failure keeps its stable code and authored message. */
  @ExceptionHandler(DomainValidationException.class)
  ResponseEntity<ApiError> handleDomainValidation(DomainValidationException exception) {
    return error(HttpStatus.BAD_REQUEST, exception.code(), exception.getMessage());
  }

  /** SECURITY-05: Bean validation failures expose allowlisted field names and codes only. */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  ResponseEntity<ApiError> handleInvalidBody(MethodArgumentNotValidException exception) {
    List<ApiError.FieldError> fieldErrors =
        exception.getBindingResult().getFieldErrors().stream()
            .map(
                error ->
                    new ApiError.FieldError(
                        error.getField(),
                        error.getCode() == null ? "INVALID" : error.getCode(),
                        error.getDefaultMessage()))
            .sorted(Comparator.comparing(ApiError.FieldError::field))
            .toList();
    return ResponseEntity.badRequest()
        .contentType(MediaType.APPLICATION_JSON)
        .body(
            ApiError.validation(
                "The request contains invalid fields", RequestCorrelation.current(), fieldErrors));
  }

  /** SECURITY-05: Method-level parameter validation is mapped to the same safe field contract. */
  @ExceptionHandler(HandlerMethodValidationException.class)
  ResponseEntity<ApiError> handleInvalidParameters(HandlerMethodValidationException ignored) {
    return validationWithoutFields("The request contains invalid parameters");
  }

  /**
   * SECURITY-05: A wrong parameter or path type is rejected without echoing the submitted value.
   */
  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  ResponseEntity<ApiError> handleTypeMismatch(MethodArgumentTypeMismatchException exception) {
    return ResponseEntity.badRequest()
        .contentType(MediaType.APPLICATION_JSON)
        .body(
            ApiError.validation(
                "The request contains an unsupported value",
                RequestCorrelation.current(),
                List.of(
                    new ApiError.FieldError(
                        exception.getName(), "TYPE_MISMATCH", "has an unsupported value"))));
  }

  @ExceptionHandler(MissingServletRequestParameterException.class)
  ResponseEntity<ApiError> handleMissingParameter(
      MissingServletRequestParameterException exception) {
    return ResponseEntity.badRequest()
        .contentType(MediaType.APPLICATION_JSON)
        .body(
            ApiError.validation(
                "The request is missing a required parameter",
                RequestCorrelation.current(),
                List.of(
                    new ApiError.FieldError(
                        exception.getParameterName(), "REQUIRED", "is required"))));
  }

  /** SECURITY-03: Parser detail such as line, column or the raw body is never returned. */
  @ExceptionHandler(HttpMessageNotReadableException.class)
  ResponseEntity<ApiError> handleUnreadableBody(HttpMessageNotReadableException ignored) {
    return error(HttpStatus.BAD_REQUEST, "MALFORMED_REQUEST", "The request body could not be read");
  }

  @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
  ResponseEntity<ApiError> handleUnsupportedMediaType(HttpMediaTypeNotSupportedException ignored) {
    return error(
        HttpStatus.UNSUPPORTED_MEDIA_TYPE,
        "UNSUPPORTED_MEDIA_TYPE",
        "The request media type is not supported");
  }

  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  ResponseEntity<ApiError> handleUnsupportedMethod(HttpRequestMethodNotSupportedException ignored) {
    return error(
        HttpStatus.METHOD_NOT_ALLOWED,
        "METHOD_NOT_ALLOWED",
        "The request method is not supported for this resource");
  }

  /** SECURITY-03: An unmatched route answers with the same shape and reveals no routing detail. */
  @ExceptionHandler(NoResourceFoundException.class)
  ResponseEntity<ApiError> handleUnknownRoute(NoResourceFoundException ignored) {
    return error(
        HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", "The requested resource was not found");
  }

  /** SECURITY-15: An unexpected failure is logged once with the correlation ID and nothing else. */
  @ExceptionHandler(Exception.class)
  ResponseEntity<ApiError> handleUnexpected(Exception exception) {
    String requestId = RequestCorrelation.current();
    log.error("unhandled_request_failure requestId={}", requestId, exception);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .contentType(MediaType.APPLICATION_JSON)
        .body(ApiError.of("INTERNAL_ERROR", "The request could not be completed", requestId));
  }

  private ResponseEntity<ApiError> validationWithoutFields(String message) {
    return ResponseEntity.badRequest()
        .contentType(MediaType.APPLICATION_JSON)
        .body(ApiError.validation(message, RequestCorrelation.current(), List.of()));
  }

  private ResponseEntity<ApiError> error(HttpStatus status, String code, String message) {
    return ResponseEntity.status(status)
        .contentType(MediaType.APPLICATION_JSON)
        .body(ApiError.of(code, message, RequestCorrelation.current()));
  }
}
