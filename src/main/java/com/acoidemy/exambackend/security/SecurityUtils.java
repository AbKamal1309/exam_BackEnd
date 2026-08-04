package com.acoidemy.exambackend.security;

import com.acoidemy.exambackend.entities.AppUser;
import com.acoidemy.exambackend.repositories.AppUserRepository;
import lombok.AllArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * Ne JAMAIS faire confiance à un id envoyé par le client (requesterId, creatorId,
 * adminId, userId, ...) pour une décision d'autorisation : on utilise systématiquement
 * l'identité résolue depuis le JWT validé (Authentication.getName() = email).
 */
@Component
@AllArgsConstructor
public class SecurityUtils {

    private final AppUserRepository appUserRepository;

    public AppUser getCurrentUser(Authentication authentication) {
        String email = authentication.getName();
        AppUser appUser = appUserRepository.findByEmail(email);
        if (appUser == null) {
            throw new IllegalStateException("Utilisateur authentifié introuvable : " + email);
        }
        return appUser;
    }

    public Long getCurrentUserId(Authentication authentication) {
        return getCurrentUser(authentication).getId();
    }

    public boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equalsIgnoreCase("ROLE_ADMIN"));
    }

    /**
     * Variante utile quand seule l'entité AppUser est disponible (pas
     * l'Authentication de la requête HTTP en cours) — ex: BillingService, appelé
     * depuis des services métier qui manipulent déjà un AppUser.
     */
    public boolean isAdminUser(AppUser user) {
        if (user.getAppRoles() == null) return false;
        return user.getAppRoles().stream()
                .anyMatch(r -> "ROLE_ADMIN".equalsIgnoreCase(r.getRoleName()));
    }
}
