package com.acoidemy.exambackend.services;

import com.acoidemy.exambackend.dtos.AdminStatsDTO;
import com.acoidemy.exambackend.dtos.AdminSubscriptionSummaryDTO;
import com.acoidemy.exambackend.dtos.AdminUserSummaryDTO;
import com.acoidemy.exambackend.entities.AppRole;
import com.acoidemy.exambackend.entities.AppUser;
import com.acoidemy.exambackend.entities.Subscription;
import com.acoidemy.exambackend.exceptions.UserNotFoundException;
import com.acoidemy.exambackend.repositories.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminService {

    private static final String ADMIN_ROLE_NAME = "ADMIN";

    // Mêmes valeurs que BillingServiceImpl, pour un revenu estimé cohérent.
    @Value("${app.manual-payment.monthly-price-mad:29}")
    private double manualMonthlyPriceMad;
    @Value("${app.manual-payment.annual-price-mad:290}")
    private double manualAnnualPriceMad;

    private final AppUserRepository appUserRepository;
    private final AppRoleRepository appRoleRepository;
    private final ExamRepository examRepository;
    private final GroupRepository groupRepository;
    private final TestExamRepository testExamRepository;
    private final QuestionRepository questionRepository;
    private final SubscriptionRepository subscriptionRepository;

    public AdminStatsDTO getStats() {
        AdminStatsDTO stats = new AdminStatsDTO();
        stats.setTotalUsers(appUserRepository.count());
        stats.setTotalExams(examRepository.count());
        stats.setTotalGroups(groupRepository.count());
        stats.setTotalQuestions(questionRepository.count());
        stats.setTotalTestsPassed(testExamRepository.count());

        long premiumUsers = appUserRepository.findAll().stream().filter(AppUser::isPremium).count();
        stats.setPremiumUsers(premiumUsers);
        stats.setFreeUsers(stats.getTotalUsers() - premiumUsers);

        // Approximatif : compte les Subscription au statut ACTIVE, sans dédupliquer
        // par utilisateur (un renouvellement peut laisser plusieurs lignes ACTIVE).
        // Suffisant pour une vue d'ensemble, pas pour une compta précise.
        List<Subscription> active = subscriptionRepository.findByStatusOrderByExpiryDateAsc("ACTIVE");
        long monthlyCount = active.stream().filter(s -> planOf(s).equals("MONTHLY")).count();
        long annualCount = active.stream().filter(s -> planOf(s).equals("ANNUAL")).count();
        long manualCount = active.stream().filter(s -> providerOf(s).equals("MANUAL")).count();
        long googlePlayCount = active.stream().filter(s -> providerOf(s).equals("GOOGLE_PLAY")).count();

        stats.setMonthlySubscriptions(monthlyCount);
        stats.setAnnualSubscriptions(annualCount);
        stats.setManualSubscriptions(manualCount);
        stats.setGooglePlaySubscriptions(googlePlayCount);
        stats.setEstimatedMonthlyRevenueMad(
                monthlyCount * manualMonthlyPriceMad + annualCount * (manualAnnualPriceMad / 12.0)
        );

        return stats;
    }

    public List<AdminUserSummaryDTO> searchUsers(String query) {
        String q = query == null ? "" : query.trim().toLowerCase();
        return appUserRepository.findAll().stream()
                .filter(u -> q.isBlank()
                        || (u.getName() != null && u.getName().toLowerCase().contains(q))
                        || (u.getEmail() != null && u.getEmail().toLowerCase().contains(q)))
                .map(u -> AdminUserSummaryDTO.builder()
                        .id(u.getId())
                        .name(u.getName())
                        .email(u.getEmail())
                        .isAdmin(u.getAppRoles() != null && u.getAppRoles().stream()
                                .anyMatch(r -> ADMIN_ROLE_NAME.equalsIgnoreCase(r.getRoleName())))
                        .isPremium(u.isPremium())
                        .premiumUntil(u.getPremiumUntil())
                        .build())
                .limit(100) // garde-fou simple, pas de pagination pour l'instant
                .toList();
    }

    public List<AdminSubscriptionSummaryDTO> getActiveSubscriptions() {
        return subscriptionRepository.findByStatusOrderByExpiryDateAsc("ACTIVE").stream()
                .map(s -> AdminSubscriptionSummaryDTO.builder()
                        .id(s.getId())
                        .userName(s.getUser() != null ? s.getUser().getName() : null)
                        .userEmail(s.getUser() != null ? s.getUser().getEmail() : null)
                        .plan(planOf(s))
                        .provider(providerOf(s))
                        .status(s.getStatus())
                        .expiryDate(s.getExpiryDate())
                        .build())
                .toList();
    }

    private String planOf(Subscription s) {
        String id = s.getProductId();
        return (id != null && id.toLowerCase().contains("annual")) ? "ANNUAL" : "MONTHLY";
    }

    private String providerOf(Subscription s) {
        String id = s.getProductId();
        return (id != null && id.startsWith("manual_")) ? "MANUAL" : "GOOGLE_PLAY";
    }

    public void promoteToAdmin(Long userId) throws UserNotFoundException {
        AppUser user = appUserRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Utilisateur introuvable"));
        AppRole adminRole = getOrCreateAdminRole();

        boolean alreadyAdmin = user.getAppRoles().stream()
                .anyMatch(r -> ADMIN_ROLE_NAME.equalsIgnoreCase(r.getRoleName()));
        if (!alreadyAdmin) {
            user.getAppRoles().add(adminRole);
            appUserRepository.save(user);
        }
    }

    // Empêche de retirer le dernier admin de la plateforme (évite le "lockout" total)
    public void demoteFromAdmin(Long userId) throws UserNotFoundException {
        AppUser user = appUserRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Utilisateur introuvable"));

        boolean isAdmin = user.getAppRoles().stream()
                .anyMatch(r -> ADMIN_ROLE_NAME.equalsIgnoreCase(r.getRoleName()));
        if (!isAdmin) return;

        long adminCount = appUserRepository.findAll().stream()
                .filter(u -> u.getAppRoles().stream()
                        .anyMatch(r -> ADMIN_ROLE_NAME.equalsIgnoreCase(r.getRoleName())))
                .count();

        if (adminCount <= 1) {
            throw new RuntimeException("Impossible de retirer le dernier compte admin de la plateforme.");
        }

        user.getAppRoles().removeIf(r -> ADMIN_ROLE_NAME.equalsIgnoreCase(r.getRoleName()));
        appUserRepository.save(user);
    }

    private AppRole getOrCreateAdminRole() {
        AppRole role = appRoleRepository.findByRoleName(ADMIN_ROLE_NAME);
        if (role == null) {
            role = new AppRole();
            role.setRoleName(ADMIN_ROLE_NAME);
            role = appRoleRepository.save(role);
        }
        return role;
    }
}
