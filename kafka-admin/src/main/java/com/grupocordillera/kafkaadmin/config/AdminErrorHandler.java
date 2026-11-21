package com.grupocordillera.kafkaadmin.config;

import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestControllerAdvice
public class AdminErrorHandler {
    private static final Logger log=LoggerFactory.getLogger(AdminErrorHandler.class);
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> badRequest(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(Map.of("error",ex.getMessage()));
    }
    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> serverError(Exception ex) {
        log.error("Error de administracion EP5",ex);
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
            .body(Map.of("error","Servicio de mensajeria no disponible o solicitud fallida"));
    }
}
