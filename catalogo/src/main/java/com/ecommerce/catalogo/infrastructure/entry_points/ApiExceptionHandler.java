package com.ecommerce.catalogo.infrastructure.entry_points;

import com.ecommerce.catalogo.domain.model.Resultado;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class ApiExceptionHandler {
    private static final Logger LOG = LoggerFactory.getLogger(ApiExceptionHandler.class);
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> validar(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Resultado.rechazo(e.getMessage()));
    }
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<?> json(HttpMessageNotReadableException e) {
        return ResponseEntity.badRequest().body(Resultado.rechazo("El cuerpo debe contener un JSON válido con los campos obligatorios"));
    }
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<?> acceso(ResponseStatusException e) {
        return ResponseEntity.status(e.getStatusCode()).body(Resultado.rechazo(e.getReason()));
    }
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<?> tecnico(RuntimeException e) {
        LOG.error("Error técnico en catálogo", e);
        return ResponseEntity.internalServerError().body(Resultado.rechazo("No fue posible procesar la solicitud por un error interno"));
    }
}
