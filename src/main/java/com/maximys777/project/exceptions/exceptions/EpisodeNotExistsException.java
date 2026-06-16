package com.maximys777.project.exceptions.exceptions;

public class EpisodeNotExistsException extends RuntimeException {
    public EpisodeNotExistsException(String message) {
        super(message);
    }

    public EpisodeNotExistsException(String message, Throwable cause) {
        super(message, cause);
    }
}