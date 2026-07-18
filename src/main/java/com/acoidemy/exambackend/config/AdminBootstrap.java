package com.acoidemy.exambackend.config;

import com.acoidemy.exambackend.entities.AppRole;
import com.acoidemy.exambackend.entities.AppUser;
import com.acoidemy.exambackend.repositories.AppRoleRepository;
import com.acoidemy.exambackend.repositories.AppUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Au démarrage (une fois l'application COMPLÈTEMENT prête — voir ApplicationReadyEvent
 * ci-dessous, qui se déclenche après TOUS les CommandLineRunner, y compris le seeder de
 * données de démo — contrairement à @Order sur un CommandLineRunner-lambda, qui n'est pas
 * fiable pour garantir l'ordre) :
 *  1. Crée le rôle ADMIN s'il n'existe pas encore.
 *  2. Si app.admin.bootstrap-email est défini (variable d'environnement), promeut le compte
 *     correspondant en ADMIN — SEULEMENT si ce compte existe déjà.
 *
 * Aucun mot de passe n'est généré ici : l'admin utilise ses identifiants habituels, juste promus.
 * Idempotent : ne fait rien si le compte est déjà admin.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AdminBootstrap {

    private static final String ADMIN_ROLE_NAME = "ADMIN";

    private final AppRoleRepository appRoleRepository;
    private final AppUserRepository appUserRepository;

    @Value("${app.admin.bootstrap-email:}")
    private String bootstrapAdminEmail;

    @EventListener(ApplicationReadyEvent.class)
    public void run() {
        AppRole adminRole = appRoleRepository.findByRoleName(ADMIN_ROLE_NAME);
        if (adminRole == null) {
            adminRole = new AppRole();
            adminRole.setRoleName(ADMIN_ROLE_NAME);
            adminRole = appRoleRepository.save(adminRole);
            log.info("Rôle ADMIN créé.");
        }

        if (bootstrapAdminEmail == null || bootstrapAdminEmail.isBlank()) {
            return;
        }

        AppUser user = appUserRepository.findByEmail(bootstrapAdminEmail.trim());
        if (user == null) {
            log.warn("app.admin.bootstrap-email défini ({}) mais aucun compte avec cet email n'existe. " +
                    "Crée d'abord le compte via l'inscription normale, puis redémarre l'application.",
                    bootstrapAdminEmail);
            return;
        }

        boolean alreadyAdmin = user.getAppRoles().stream()
                .anyMatch(r -> ADMIN_ROLE_NAME.equalsIgnoreCase(r.getRoleName()));

        if (!alreadyAdmin) {
            user.getAppRoles().add(adminRole);
            appUserRepository.save(user);
            log.info("Utilisateur {} promu ADMIN au démarrage.", bootstrapAdminEmail);
        }
    }
}
