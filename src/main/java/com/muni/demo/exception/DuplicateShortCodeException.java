package com.muni.demo.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when a custom alias or short code already exists.
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class DuplicateShortCodeException extends RuntimeException {
    public DuplicateShortCodeException(String message) {
        super(message);
    }
}
