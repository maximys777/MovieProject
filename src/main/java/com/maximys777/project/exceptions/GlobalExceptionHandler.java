package com.maximys777.project.exceptions;

import com.maximys777.project.exceptions.dto.response.ErrorResponse;
import com.maximys777.project.exceptions.exceptions.AlreadyExistsException;
import com.maximys777.project.exceptions.exceptions.EpisodeNotExistsException;
import com.maximys777.project.exceptions.exceptions.InvalidInputException;
import com.maximys777.project.exceptions.exceptions.MovieNotFoundException;
import com.maximys777.project.exceptions.exceptions.ResourceNotFoundException;
import com.maximys777.project.exceptions.exceptions.SeasonNotExistsException;
import com.maximys777.project.exceptions.exceptions.ServiceUnavailable;
import com.maximys777.project.exceptions.exceptions.TvShowNotExistsException;
import com.maximys777.project.exceptions.exceptions.TvShowNotFoundException;
import com.maximys777.project.exceptions.exceptions.UsernameNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import java.time.Instant;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(ResourceNotFoundException ex, WebRequest request) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    @ExceptionHandler(ServiceUnavailable.class)
    public ResponseEntity<ErrorResponse> handleServiceUnavailableException(ServiceUnavailable ex, WebRequest request) {
        return build(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage(), request);
    }

    @ExceptionHandler(AlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleAlreadyExistsException(AlreadyExistsException ex, WebRequest request) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), request);
    }

    @ExceptionHandler(MovieNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleMovieNotFoundException(MovieNotFoundException ex, WebRequest request) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    @ExceptionHandler(InvalidInputException.class)
    public ResponseEntity<ErrorResponse> handleInvalidInputException(InvalidInputException ex, WebRequest request) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    @ExceptionHandler(TvShowNotExistsException.class)
    public ResponseEntity<ErrorResponse> handleTvShowNotExistsException(TvShowNotExistsException ex, WebRequest request) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    @ExceptionHandler(TvShowNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleTvShowNotFoundException(TvShowNotFoundException ex, WebRequest request) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    @ExceptionHandler(SeasonNotExistsException.class)
    public ResponseEntity<ErrorResponse> handleSeasonNotExistsException(SeasonNotExistsException ex, WebRequest request) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    @ExceptionHandler(EpisodeNotExistsException.class)
    public ResponseEntity<ErrorResponse> handleEpisodeNotExistsException(EpisodeNotExistsException ex, WebRequest request) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUsernameNotFoundException(org.springframework.security.core.userdetails.UsernameNotFoundException ex, WebRequest request) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String message, WebRequest request) {
        ErrorResponse errorResponse = ErrorResponse.builder().timestamp(Instant.now()).status(status.value()).message(message).path(request.getLocale().getLanguage()).build();

        return ResponseEntity.status(status).body(errorResponse);
    }

}