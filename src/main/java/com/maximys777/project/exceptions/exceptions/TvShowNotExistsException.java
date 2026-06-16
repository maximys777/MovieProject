package com.maximys777.project.exceptions.exceptions;

public class TvShowNotExistsException extends RuntimeException {
    public TvShowNotExistsException(String message) {
        super(message);
    }

    public TvShowNotExistsException(String message, Throwable cause) {
        super(message, cause);
    }
}