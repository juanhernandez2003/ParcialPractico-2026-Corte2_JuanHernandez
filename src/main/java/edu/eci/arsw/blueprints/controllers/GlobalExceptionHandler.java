package edu.eci.arsw.blueprints.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Traduce los errores de entrada de las peticiones POST y PUT a 400 Bad Request.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    // Falla de @Valid: campos vacios, nulos o puntos incompletos
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> details = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(fe -> details.putIfAbsent(fe.getField(), fe.getDefaultMessage()));
        return ResponseEntity.badRequest().body(Map.of("error", "Invalid request data", "details", details));
    }

    // Body ausente, JSON mal formado o tipos incorrectos (ej. "x":"abc")
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> handleUnreadable(HttpMessageNotReadableException ex) {
        return ResponseEntity.badRequest().body(Map.of("error", "Malformed or missing request body"));
    }
}
