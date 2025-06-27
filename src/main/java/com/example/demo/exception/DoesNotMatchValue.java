package com.example.demo.exception;

public class DoesNotMatchValue extends RuntimeException {
    public DoesNotMatchValue(String message) {
        super(message);
    }
}
