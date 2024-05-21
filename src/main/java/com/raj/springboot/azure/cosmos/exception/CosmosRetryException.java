package com.raj.springboot.azure.cosmos.exception;

public class CosmosRetryException extends RuntimeException {

    public CosmosRetryException(String message) {
        super(message);
    }

    public CosmosRetryException(String message, Exception cause) {
        super(message, cause);
    }
}
