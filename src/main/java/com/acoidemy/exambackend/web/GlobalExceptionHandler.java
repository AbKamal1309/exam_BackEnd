package com.acoidemy.exambackend.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Centralise la traduction des RuntimeException "métier" (ex: "Vous n'êtes pas
 * membre de ce groupe.", "Seul un admin peut partager un examen.", etc.) en
 * réponses HTTP propres avec un message JSON exploitable côté client — au lieu
 * de les laisser remonter en 500 brut jusqu'au conteneur Servlet (ce qui était
 * le cas jusqu'ici : aucun @ControllerAdvice global n'existait, seul un
 * @ExceptionHandler local à TestRestController gérait un cas très spécifique).
 *
 * Un @ExceptionHandler défini directement dans un contrôleur reste prioritaire
 * sur celui-ci pour les exceptions qu'il cible explicitement — pas de conflit
 * avec l'existant.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(RuntimeException ex) {
        HttpStatus status = classify(ex.getMessage());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", ex.getMessage() != null ? ex.getMessage() : "Une erreur est survenue.");

        return ResponseEntity.status(status).body(body);
    }

    /**
     * La quasi-totalité des RuntimeException métier de ce projet sont des refus
     * d'accès (permissions, appartenance à un groupe/examen) → 403 par défaut.
     * Quelques mots-clés suffisent à distinguer les cas "ressource introuvable"
     * (404) ; on reste volontairement simple plutôt que de créer une hiérarchie
     * d'exceptions dédiées pour chaque cas.
     */
    private HttpStatus classify(String message) {
        if (message == null) return HttpStatus.INTERNAL_SERVER_ERROR;
        String m = message.toLowerCase();
        if (m.contains("n'existe pas") || m.contains("introuvable") || m.contains("not found")) {
            return HttpStatus.NOT_FOUND;
        }
        return HttpStatus.FORBIDDEN;
    }
}