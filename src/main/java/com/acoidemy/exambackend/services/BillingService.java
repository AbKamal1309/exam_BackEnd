package com.acoidemy.exambackend.services;

import com.acoidemy.exambackend.dtos.ManualPaymentInstructionsDTO;
import com.acoidemy.exambackend.dtos.ManualPremiumRequestCreateDTO;
import com.acoidemy.exambackend.dtos.ManualPremiumRequestDTO;
import com.acoidemy.exambackend.dtos.SubscriptionStatusDTO;
import com.acoidemy.exambackend.dtos.VerifyPurchaseRequestDTO;
import com.acoidemy.exambackend.entities.AppUser;
import org.springframework.security.core.Authentication;

import java.util.List;

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

    // ── Activation premium manuelle (solution de repli, voir ManualPremiumRequest) ──

    /** Infos affichées à l'utilisateur pour payer hors-app (RIB, WhatsApp, tarifs). */
    ManualPaymentInstructionsDTO getManualPaymentInstructions();

    /** Crée une demande PENDING pour l'utilisateur authentifié. */
    ManualPremiumRequestDTO createManualRequest(ManualPremiumRequestCreateDTO dto, Authentication authentication);

    /** Dernière demande PENDING de l'utilisateur authentifié, ou null s'il n'en a aucune. */
    ManualPremiumRequestDTO getMyPendingManualRequest(Authentication authentication);

    /** Réservé admin : liste des demandes en attente, les plus anciennes en premier. */
    List<ManualPremiumRequestDTO> getPendingManualRequests(Authentication authentication);

    /** Réservé admin : valide la demande, active premium pour l'utilisateur concerné. */
    ManualPremiumRequestDTO approveManualRequest(Long requestId, Authentication authentication);

    /** Réservé admin : rejette la demande, ne modifie pas le statut premium de l'utilisateur. */
    ManualPremiumRequestDTO rejectManualRequest(Long requestId, Authentication authentication);
}
