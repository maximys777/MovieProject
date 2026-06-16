package com.maximys777.project.exceptions.exceptions;

public class SeasonNotExistsException extends RuntimeException {
    public SeasonNotExistsException(String message) {
        super(message);
    }

    public SeasonNotExistsException(String message, Throwable cause) {
        super(message, cause);
    }
}