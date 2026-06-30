package com.muni.demo.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when the provided URL is malformed or invalid.
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidURLException extends RuntimeException {
    public InvalidURLException(String message) {
        super(message);
    }
}
