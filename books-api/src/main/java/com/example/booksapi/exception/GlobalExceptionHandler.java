package com.example.booksapi.exception;

import com.example.booksapi.dto.ApiProblem;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final MediaType PROBLEM_JSON = MediaType.valueOf("application/problem+json");

    @ExceptionHandler(BookNotFoundException.class)
    public ResponseEntity<ApiProblem> handleBookNotFound(BookNotFoundException ex, HttpServletRequest req) {
        return problem(HttpStatus.NOT_FOUND, "/problems/book-not-found", "Book Not Found", ex.getMessage(), req);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiProblem> handleConflict(ConflictException ex, HttpServletRequest req) {
        return problem(HttpStatus.CONFLICT, "/problems/conflict", "Conflict", ex.getMessage(), req);
    }

    @ExceptionHandler(PreconditionFailedException.class)
    public ResponseEntity<ApiProblem> handlePreconditionFailed(PreconditionFailedException ex, HttpServletRequest req) {
        return problem(HttpStatus.PRECONDITION_FAILED, "/problems/precondition-failed", "Precondition Failed", ex.getMessage(), req);
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiProblem> handleUnauthorized(UnauthorizedException ex, HttpServletRequest req) {
        return problem(HttpStatus.UNAUTHORIZED, "/problems/unauthorized", "Unauthorized", ex.getMessage(), req);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiProblem> handleBadCredentials(BadCredentialsException ex, HttpServletRequest req) {
        return problem(HttpStatus.UNAUTHORIZED, "/problems/invalid-credentials", "Invalid Credentials",
                "Username or password is incorrect", req);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiProblem> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        String detail = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        return problem(HttpStatus.BAD_REQUEST, "/problems/validation-error", "Validation Error", detail, req);
    }

    // Catch-all — never expose stack traces
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiProblem> handleGeneric(Exception ex, HttpServletRequest req) {
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "/problems/internal-error",
                "Internal Server Error", "An unexpected error occurred", req);
    }

    private ResponseEntity<ApiProblem> problem(HttpStatus status, String type, String title, String detail,
                                               HttpServletRequest req) {
        ApiProblem body = ApiProblem.builder()
                .type(type)
                .title(title)
                .status(status.value())
                .detail(detail)
                .instance(req.getRequestURI())
                .build();
        return ResponseEntity.status(status).contentType(PROBLEM_JSON).body(body);
    }
}
