package com.maximys777.project.exceptions.exceptions;

public class TvShowNotFoundException extends RuntimeException {
    public TvShowNotFoundException(String message) {
        super(message);
    }

    public TvShowNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}