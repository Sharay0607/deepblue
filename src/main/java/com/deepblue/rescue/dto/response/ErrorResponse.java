package com.deepblue.rescue.dto.response;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Contrato único de error de la API: todos los errores tienen esta misma estructura.
 */
public record ErrorResponse(
        LocalDateTime timestamp,
        int status,
        String error,
        String message,
        Map<String, String> details
) {
}
