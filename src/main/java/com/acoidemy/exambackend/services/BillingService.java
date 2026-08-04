package com.acoidemy.exambackend.services;

import com.acoidemy.exambackend.dtos.SubscriptionStatusDTO;
import com.acoidemy.exambackend.dtos.VerifyPurchaseRequestDTO;
import com.acoidemy.exambackend.entities.AppUser;
import org.springframework.security.core.Authentication;

public interface BillingService {

    /**
     * Vérifie un achat auprès de Google Play (jamais de confiance aveugle dans ce
     * que le client envoie) et met à jour AppUser.premiumUntil en conséquence.
     */
    SubscriptionStatusDTO verifyPurchase(VerifyPurchaseRequestDTO request, Authentication authentication) throws Exception;

    /** Statut actuel (premium ou non, quotas restants) pour l'utilisateur authentifié. */
    SubscriptionStatusDTO getStatus(Authentication authentication);

    /**
     * Lève une RuntimeException si l'utilisateur (compte gratuit, non admin) a déjà
     * épuisé son quota mensuel de génération IA. Ne fait rien pour un compte
     * premium ou un admin.
     */
    void checkAiQuota(AppUser user);

    /** Incrémente le compteur de génération IA (à appeler après un succès). */
    void incrementAiUsage(AppUser user);

    /**
     * Lève une RuntimeException si l'utilisateur (compte gratuit, non admin) a déjà
     * atteint sa limite de groupes créés.
     */
    void checkGroupQuota(AppUser user);
}
