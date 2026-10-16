package com.ecommerce.auth.infrastructure.entry_points;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice(assignableTypes = SaldoController.class)
public class SaldoExceptionHandler {
    private static final Logger LOG = LoggerFactory.getLogger(SaldoExceptionHandler.class);
    @ExceptionHandler({IllegalArgumentException.class, HttpMessageNotReadableException.class})
    public ResponseEntity<?> datos(RuntimeException e) {
        return ResponseEntity.badRequest().body(new SaldoController.SaldoResponse(false, "El monto y el identificador de operación deben ser válidos", null));
    }
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<?> acceso(ResponseStatusException e) {
        return ResponseEntity.status(e.getStatusCode()).body(new SaldoController.SaldoResponse(false, e.getReason(), null));
    }
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<?> tecnico(RuntimeException e) {
        LOG.error("Error técnico al modificar saldo", e);
        return ResponseEntity.internalServerError().body(new SaldoController.SaldoResponse(false, "No fue posible procesar la solicitud por un error interno", null));
    }
}
