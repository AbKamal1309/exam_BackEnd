package com.acoidemy.exambackend.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

/**
 * Sans ce bean, Spring Security répond 403 Forbidden par défaut (pas 401) sur
 * toute requête sans authentification valide — comportement par défaut d'une
 * config stateless sans formLogin()/httpBasic(), faute de savoir "où rediriger"
 * un utilisateur non connecté. Ce point d'entrée force explicitement un vrai
 * 401 Unauthorized, pour que les clients (Angular/Android) puissent distinguer
 * "token expiré/absent" (401 → tenter un refresh) d'un vrai refus d'autorisation
 * métier (403 → ex. "ce n'est pas ton examen", jamais résolu par un refresh).
 */
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                          AuthenticationException authException) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(
                Map.of("message", "Authentification requise ou token invalide/expiré.")
        ));
    }
}
