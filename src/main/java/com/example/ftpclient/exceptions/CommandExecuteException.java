package com.example.ftpclient.exceptions;

public class CommandExecuteException extends RuntimeException{
    public CommandExecuteException() {
    }

    public CommandExecuteException(String message) {
        super(message);
    }

    public CommandExecuteException(String message, Throwable cause) {
        super(message, cause);
    }

    public CommandExecuteException(Throwable cause) {
        super(cause);
    }

    public CommandExecuteException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
