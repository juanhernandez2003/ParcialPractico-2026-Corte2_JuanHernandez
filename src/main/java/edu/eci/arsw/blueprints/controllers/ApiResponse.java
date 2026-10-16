package edu.eci.arsw.blueprints.controllers;

/**
 * Respuesta uniforme de la API.
 * Ejemplo: { "code": 200, "message": "execute ok", "data": { ... } }
 *
 * @param code    codigo HTTP de la respuesta
 * @param message descripcion corta del resultado
 * @param data    contenido de la respuesta (null cuando hay error)
 */
public record ApiResponse<T>(int code, String message, T data) { }
