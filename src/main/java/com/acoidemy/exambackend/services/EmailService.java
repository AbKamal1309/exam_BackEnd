package com.acoidemy.exambackend.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * Envoi d'emails transactionnels via l'API HTTP de Brevo (pas de SMTP).
 *
 * Choix volontaire : Render bloque le trafic sortant sur les ports SMTP
 * (25/465/587) sur son plan gratuit depuis fin 2025. L'API REST de Brevo
 * passe en HTTPS classique (port 443), qui n'est jamais bloqué — donc pas
 * besoin de passer sur un plan Render payant.
 *
 * Doc API : https://developers.brevo.com/reference/sendtransacemail
 */
@Service
@Slf4j
public class EmailService {

    private static final String BREVO_API_URL = "https://api.brevo.com/v3/smtp/email";

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${app.brevo.api-key}")
    private String brevoApiKey;

    @Value("${app.mail.from:no-reply@amtihan.app}")
    private String fromAddress;

    @Value("${app.mail.from-name:Amtihan}")
    private String fromName;

    public void sendPasswordResetCode(String toEmail, String code) {
        String textContent =
            "Bonjour,\n\n" +
            "Voici votre code de réinitialisation de mot de passe Amtihan :\n\n" +
            "    " + code + "\n\n" +
            "Ce code est valable 15 minutes. Si vous n'êtes pas à l'origine de cette demande, " +
            "vous pouvez ignorer cet email en toute sécurité.\n\n" +
            "— L'équipe Amtihan";

        Map<String, Object> body = Map.of(
            "sender", Map.of("name", fromName, "email", fromAddress),
            "to", List.of(Map.of("email", toEmail)),
            "subject", "Amtihan — Code de réinitialisation de mot de passe",
            "textContent", textContent
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("api-key", brevoApiKey);
        headers.set("accept", "application/json");

        try {
            restTemplate.postForEntity(BREVO_API_URL, new HttpEntity<>(body, headers), String.class);
        } catch (Exception e) {
            // On log l'échec d'envoi mais on ne le remonte jamais tel quel au client
            // (voir AuthController.forgotPassword — réponse générique dans tous les cas).
            log.error("Échec d'envoi de l'email de reset à {} : {}", toEmail, e.getMessage());
        }
    }
}
