package com.bintech.metrix.exception;

public class FrozenUserException extends RuntimeException {
    public FrozenUserException(String message) {
        super(message);
    }
}
