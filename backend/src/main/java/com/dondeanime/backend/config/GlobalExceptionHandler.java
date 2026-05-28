package com.dondeanime.backend.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Contrato de error único para toda la API (RFC 7807 / ProblemDetail).
 *
 * Extiende ResponseEntityExceptionHandler para que las excepciones de
 * framework (validación, body ilegible, ResponseStatusException vía
 * ErrorResponseException, 404/405/415...) sigan devolviendo su status
 * correcto en formato ProblemDetail. El handler de Exception solo captura
 * lo verdaderamente inesperado y nunca filtra detalles internos al cliente.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex) {
        log.error("Error no controlado en la API", ex);
        return ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Error interno del servidor");
    }
}
