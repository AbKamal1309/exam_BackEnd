package com.acoidemy.exambackend.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Demande d'activation premium manuelle : solution de repli pendant que le
 * compte marchand Google Play (bloqué pour le Maroc) et l'intégration d'une
 * passerelle de paiement en ligne (ChariBaaS / CMI / etc.) sont en cours de
 * résolution. L'utilisateur paie hors-app (virement, WhatsApp) et un admin
 * valide manuellement — voir BillingServiceImpl.approveManualRequest().
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ManualPremiumRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "app_user_id")
    private AppUser user;

    private String plan; // "MONTHLY" | "ANNUAL"

    // Référence libre laissée par l'utilisateur (n° de virement, capture WhatsApp
    // mentionnée, etc.) — purement informatif, ne fait foi de rien en soi.
    @Column(length = 500)
    private String paymentReference;

    private String status = "PENDING"; // PENDING | APPROVED | REJECTED

    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime processedAt;

    // Id de l'admin ayant traité la demande, pour traçabilité.
    private Long processedByUserId;
}
