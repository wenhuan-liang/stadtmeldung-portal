package com.stadtmeldung.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> badRequest(
            IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(
                Map.of("message", exception.getMessage()));
    }

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<Map<String, String>> forbidden(
            SecurityException exception) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                Map.of("message", "Diese Aktion ist nicht erlaubt."));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> responseStatus(
            ResponseStatusException exception) {
        String message = exception.getReason() == null
                ? "Die Anfrage konnte nicht verarbeitet werden."
                : exception.getReason();
        return ResponseEntity.status(exception.getStatusCode()).body(
                Map.of("message", message));
    }
}
