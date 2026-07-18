package com.acoidemy.exambackend.web;

import com.acoidemy.exambackend.dtos.AdminStatsDTO;
import com.acoidemy.exambackend.exceptions.ExamNotFoundException;
import com.acoidemy.exambackend.exceptions.UserNotFoundException;
import com.acoidemy.exambackend.services.AdminService;
import com.acoidemy.exambackend.services.ExamService;
import com.acoidemy.exambackend.services.GroupService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

// Toutes les routes de ce contrôleur exigent le rôle ADMIN — un seul point de contrôle.
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;
    private final ExamService examService;
    private final GroupService groupService;

    @GetMapping("/stats")
    public AdminStatsDTO getStats() {
        return adminService.getStats();
    }

    @PostMapping("/users/{userId}/promote")
    public ResponseEntity<Void> promote(@PathVariable Long userId) throws UserNotFoundException {
        adminService.promoteToAdmin(userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/users/{userId}/demote")
    public ResponseEntity<Void> demote(@PathVariable Long userId) throws UserNotFoundException {
        adminService.demoteFromAdmin(userId);
        return ResponseEntity.noContent().build();
    }

    // Suppression d'un examen quel qu'en soit le propriétaire (modération)
    @DeleteMapping("/exams/{codeExam}")
    public ResponseEntity<Void> deleteAnyExam(@PathVariable String codeExam) throws ExamNotFoundException {
        examService.adminDeleteExam(codeExam);
        return ResponseEntity.noContent().build();
    }

    // Suppression d'un groupe quel qu'en soit le créateur (modération)
    @DeleteMapping("/groups/{groupId}")
    public ResponseEntity<Void> deleteAnyGroup(@PathVariable Long groupId) {
        groupService.adminDeleteGroup(groupId);
        return ResponseEntity.noContent().build();
    }
}
