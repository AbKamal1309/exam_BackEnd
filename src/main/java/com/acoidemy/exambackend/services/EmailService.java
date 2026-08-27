package com.acoidemy.exambackend.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Envoi d'emails transactionnels via Brevo (SMTP relay).
 * Configuration dans application.properties (voir spring.mail.*), toutes
 * les valeurs sensibles passent par des variables d'environnement.
 */
@Service
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from:no-reply@amtihan.app}")
    private String fromAddress;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendPasswordResetCode(String toEmail, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(toEmail);
        message.setSubject("Amtihan — Code de réinitialisation de mot de passe");
        message.setText(
            "Bonjour,\n\n" +
            "Voici votre code de réinitialisation de mot de passe Amtihan :\n\n" +
            "    " + code + "\n\n" +
            "Ce code est valable 15 minutes. Si vous n'êtes pas à l'origine de cette demande, " +
            "vous pouvez ignorer cet email en toute sécurité.\n\n" +
            "— L'équipe Amtihan"
        );

        try {
            mailSender.send(message);
        } catch (Exception e) {
            // On log l'échec d'envoi mais on ne le remonte jamais tel quel au client
            // (voir AuthController.forgotPassword — réponse générique dans tous les cas).
            log.error("Échec d'envoi de l'email de reset à {} : {}", toEmail, e.getMessage());
        }
    }
}
