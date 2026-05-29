package com.dondeanime.backend.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
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
        // Excepciones anotadas con @ResponseStatus (p.ej. EmbeddingClientDisabledException
        // cuando el chatbot esta apagado) llevan su propio status. El catch-all de
        // Exception las interceptaba antes de que Spring honrara la anotacion y
        // devolvia 500; aqui respetamos el status declarado sin filtrar el mensaje
        // interno al cliente.
        ResponseStatus annotated = AnnotatedElementUtils.findMergedAnnotation(
                ex.getClass(), ResponseStatus.class);
        if (annotated != null) {
            log.warn("Excepcion con @ResponseStatus {}: {}",
                    annotated.code(), ex.getClass().getSimpleName());
            if (StringUtils.hasText(annotated.reason())) {
                return ProblemDetail.forStatusAndDetail(annotated.code(), annotated.reason());
            }
            return ProblemDetail.forStatus(annotated.code());
        }
        log.error("Error no controlado en la API", ex);
        return ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Error interno del servidor");
    }
}
