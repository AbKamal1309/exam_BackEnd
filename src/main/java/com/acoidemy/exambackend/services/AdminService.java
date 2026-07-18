package com.acoidemy.exambackend.services;

import com.acoidemy.exambackend.dtos.AdminStatsDTO;
import com.acoidemy.exambackend.entities.AppRole;
import com.acoidemy.exambackend.entities.AppUser;
import com.acoidemy.exambackend.exceptions.UserNotFoundException;
import com.acoidemy.exambackend.repositories.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminService {

    private static final String ADMIN_ROLE_NAME = "ADMIN";

    private final AppUserRepository appUserRepository;
    private final AppRoleRepository appRoleRepository;
    private final ExamRepository examRepository;
    private final GroupRepository groupRepository;
    private final TestExamRepository testExamRepository;
    private final QuestionRepository questionRepository;

    public AdminStatsDTO getStats() {
        AdminStatsDTO stats = new AdminStatsDTO();
        stats.setTotalUsers(appUserRepository.count());
        stats.setTotalExams(examRepository.count());
        stats.setTotalGroups(groupRepository.count());
        stats.setTotalQuestions(questionRepository.count());
        stats.setTotalTestsPassed(testExamRepository.count());
        return stats;
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
