package com.acoidemy.exambackend.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Trace de chaque achat d'abonnement vérifié auprès de Google Play. Sert à :
 * 1. Éviter de créditer deux fois le même purchaseToken (idempotence) si le
 *    client renvoie la vérification plusieurs fois (retry réseau, etc.).
 * 2. Garder un historique consultable (support client, litiges, statistiques).
 *
 * Ne JAMAIS faire confiance à un champ "isPremium" envoyé par le mobile : ce
 * statut est déduit uniquement de AppUser.premiumUntil, lui-même mis à jour
 * uniquement après vérification serveur du purchaseToken auprès de Google.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "app_user_id")
    private AppUser user;

    private String productId;     // "premium_monthly" | "premium_annual"

    @Column(unique = true, nullable = false)
    private String purchaseToken; // identifiant unique Google Play de cet achat

    private String orderId;
    private LocalDateTime startDate;
    private LocalDateTime expiryDate;
    private String status;        // ACTIVE | EXPIRED | CANCELLED
    private LocalDateTime createdAt = LocalDateTime.now();
}
