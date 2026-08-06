package com.acoidemy.exambackend.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubscriptionStatusDTO {
    private boolean premium;
    private LocalDateTime premiumUntil; // null si compte gratuit

    // Plan de l'abonnement actif ("MONTHLY" | "ANNUAL"), déduit de la dernière
    // Subscription enregistrée — null si compte gratuit.
    private String currentPlan;
    // "GOOGLE_PLAY" | "MANUAL" — d'où vient l'abonnement actif.
    private String provider;

    // Quotas — pertinents seulement si premium == false (illimité sinon)
    private int aiQuestionsUsedThisMonth;
    private int aiQuestionsQuota;       // 10
    private int groupsCreated;
    private int groupsQuota;            // 1
}
