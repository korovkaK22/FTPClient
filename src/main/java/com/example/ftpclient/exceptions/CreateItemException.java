package com.example.ftpclient.exceptions;

public class CreateItemException extends RuntimeException{
    public CreateItemException() {
    }

    public CreateItemException(String message) {
        super(message);
    }

    public CreateItemException(String message, Throwable cause) {
        super(message, cause);
    }

    public CreateItemException(Throwable cause) {
        super(cause);
    }

    public CreateItemException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
