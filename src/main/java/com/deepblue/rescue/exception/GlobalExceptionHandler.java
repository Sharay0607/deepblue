package com.deepblue.rescue.exception;

import com.deepblue.rescue.dto.response.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Traduce las excepciones de la aplicación a un contrato HTTP consistente.
 * Todos los métodos retornan {@code ResponseEntity<ErrorResponse>}.
 *
 * <pre>
 * MethodArgumentNotValidException        → 400
 * HttpMessageNotReadableException        → 400
 * MethodArgumentTypeMismatchException    → 400
 * ResourceNotFoundException              → 404
 * BusinessRuleException                  → 409
 * Exception (cualquier otra)             → 500
 * </pre>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ------------------------------------------------------------------ 404

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(ResourceNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), Map.of());
    }

    // ------------------------------------------------------------------ 409

    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ErrorResponse> handleBusinessRule(BusinessRuleException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), Map.of());
    }

    // ------------------------------------------------------------------ 400

    /** Bean Validation sobre el body (@Valid @RequestBody). */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> details = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        error -> Objects.requireNonNullElse(error.getDefaultMessage(), "Invalid value"),
                        (first, second) -> first));

        return build(HttpStatus.BAD_REQUEST, "Request validation failed", details);
    }

    /** JSON mal formado o valor de enum inexistente en el body. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleMessageNotReadable(HttpMessageNotReadableException ex) {
        return build(HttpStatus.BAD_REQUEST,
                "Malformed or invalid JSON request",
                Map.of("body", "Check JSON syntax and enum values"));
    }

    /** Query parameter o path variable con un valor que no se puede convertir al tipo esperado. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        Map<String, String> details = Map.of(
                ex.getName(), "Invalid value: " + ex.getValue());

        return build(HttpStatus.BAD_REQUEST, "Invalid request parameter", details);
    }

    // ------------------------------------------------- errores de enrutamiento
    // Sin estos dos handlers, el handler genérico de abajo convertiría una URL
    // inexistente o un método HTTP no permitido en un 500 engañoso.

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResourceFound(NoResourceFoundException ex) {
        return build(HttpStatus.NOT_FOUND, "Resource not found", Map.of());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        return build(HttpStatus.METHOD_NOT_ALLOWED,
                "HTTP method not supported for this endpoint",
                Map.of("method", String.valueOf(ex.getMethod())));
    }

    // ------------------------------------------------------------------ 500

    /**
     * Error inesperado. No se expone al cliente el stack trace, SQL ni detalles internos.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpectedException(Exception ex) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred", Map.of());
    }

    // -------------------------------------------------------------- helper

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String message,
                                                Map<String, String> details) {
        ErrorResponse body = new ErrorResponse(
                LocalDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                details);

        return ResponseEntity.status(status).body(body);
    }
}
