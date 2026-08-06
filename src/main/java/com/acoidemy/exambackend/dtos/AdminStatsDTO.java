package com.acoidemy.exambackend.dtos;

import lombok.Data;

@Data
public class AdminStatsDTO {
    private long totalUsers;
    private long totalExams;
    private long totalGroups;
    private long totalQuestions;
    private long totalTestsPassed;

    // ── Abonnements (ajout) ──────────────────────────────────────────
    private long premiumUsers;
    private long freeUsers;
    private long monthlySubscriptions;  // approximatif : compte les Subscription ACTIVE, pas dédupliqué par utilisateur
    private long annualSubscriptions;
    private long manualSubscriptions;
    private long googlePlaySubscriptions;
    // Mensualisé : (abonnements mensuels × prix mensuel) + (abonnements annuels × prix annuel / 12)
    private double estimatedMonthlyRevenueMad;
}
