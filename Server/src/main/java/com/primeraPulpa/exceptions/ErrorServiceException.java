package com.primeraPulpa.exceptions;

public class ErrorServiceException extends RuntimeException {
    public ErrorServiceException(String message) {
        super(message);
    }
}
