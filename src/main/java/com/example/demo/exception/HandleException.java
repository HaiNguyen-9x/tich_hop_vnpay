package com.example.demo.exception;

import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import java.util.Date;

@Log4j2
@ControllerAdvice
public class HandleException {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<DetailsException> handleMethodArgumentNotValidException (MethodArgumentNotValidException methodArgumentNotValidException,
                                                                       WebRequest webRequest) {
        log.error("Validation failed: {}", methodArgumentNotValidException.getMessage());
        DetailsException exception = new DetailsException(
                new Date(),
                methodArgumentNotValidException.getFieldError().getDefaultMessage(),
                webRequest.getDescription(false)
        );
        return new ResponseEntity<>(exception, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(DoesNotMatchValue.class)
    public ResponseEntity<DetailsException> handleDoesNotMatchValue (DoesNotMatchValue doesNotMatchValue,
                                                                                   WebRequest webRequest) {
        log.error("DoesNotMatchValue Exception at {} - {}", webRequest.getDescription(false), doesNotMatchValue.getMessage());
        DetailsException exception = new DetailsException(
                new Date(),
                doesNotMatchValue.getMessage(),
                webRequest.getDescription(false)
        );
        return new ResponseEntity<>(exception, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<DetailsException> handleResourceNotFoundException (ResourceNotFoundException resourceNotFoundException,
                                                                                   WebRequest webRequest) {
        log.warn("ResourceNotFound at {} - {}", webRequest.getDescription(false), resourceNotFoundException.getMessage());
        DetailsException exception = new DetailsException(
                new Date(),
                resourceNotFoundException.getMessage(),
                webRequest.getDescription(false)
        );
        return new ResponseEntity<>(exception, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<DetailsException> handleGlobalException (Exception e,
                                                                   WebRequest webRequest) {
        DetailsException exception = new DetailsException(
                new Date(),
                e.getMessage(),
                webRequest.getDescription(false)
        );
        return new ResponseEntity<>(exception, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
