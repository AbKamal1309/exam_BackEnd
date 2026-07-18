package com.acoidemy.exambackend.web;


import com.acoidemy.exambackend.dtos.GroupRequestDTO;
import com.acoidemy.exambackend.dtos.GroupResponseDTO;
import com.acoidemy.exambackend.dtos.JoinRequestDTO;
import com.acoidemy.exambackend.security.SecurityUtils;
import com.acoidemy.exambackend.services.GroupService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/groups")
@RequiredArgsConstructor
public class GroupController {

    private final GroupService groupService;
    private final SecurityUtils securityUtils;

    // ── POST /groups ──────────────────────────────────────────────
    // Créer un nouveau groupe (le créateur = l'utilisateur authentifié)
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<GroupResponseDTO> createGroup(@RequestBody GroupRequestDTO dto,
                                                          Authentication authentication) {
        dto.setCreatorId(securityUtils.getCurrentUserId(authentication));
        return ResponseEntity.ok(groupService.createGroup(dto));
    }

    // ── GET /groups ───────────────────────────────────────────────
    @GetMapping
    public ResponseEntity<List<GroupResponseDTO>> getAllGroups() {
        return ResponseEntity.ok(groupService.getAllGroups());
    }

    // ── GET /groups/{groupId} ─────────────────────────────────────
    @GetMapping("/{groupId}")
    public ResponseEntity<GroupResponseDTO> getGroup(@PathVariable Long groupId) {
        return ResponseEntity.ok(groupService.getGroup(groupId));
    }

    // ── GET /groups/creator/{creatorId} ───────────────────────────
    @GetMapping("/creator/{creatorId}")
    public ResponseEntity<List<GroupResponseDTO>> getGroupsByCreator(@PathVariable Long creatorId) {
        return ResponseEntity.ok(groupService.getGroupsByCreator(creatorId));
    }

    // ── GET /groups/member/{userId} ───────────────────────────────
    @GetMapping("/member/{userId}")
    public ResponseEntity<List<GroupResponseDTO>> getGroupsByMember(@PathVariable Long userId) {
        return ResponseEntity.ok(groupService.getGroupsByMember(userId));
    }

    // ── POST /groups/{groupId}/members/{userId} ───────────────────
    // Ajouter un membre. "requesterId" n'est plus lu depuis la requête :
    // on utilise l'utilisateur authentifié (le service vérifie qu'il est admin/créateur).
    @PostMapping("/{groupId}/members/{userId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<GroupResponseDTO> addMember(
            @PathVariable Long groupId,
            @PathVariable Long userId,
            Authentication authentication
    ) {
        Long requesterId = securityUtils.getCurrentUserId(authentication);
        return ResponseEntity.ok(groupService.addMember(groupId, userId, requesterId));
    }

    // ── DELETE /groups/{groupId}/members/{userId} ─────────────────
    @DeleteMapping("/{groupId}/members/{userId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<GroupResponseDTO> removeMember(
            @PathVariable Long groupId,
            @PathVariable Long userId,
            Authentication authentication
    ) {
        Long requesterId = securityUtils.getCurrentUserId(authentication);
        return ResponseEntity.ok(groupService.removeMember(groupId, userId, requesterId));
    }

    // ── POST /groups/{groupId}/admins/{userId} ────────────────────
    @PostMapping("/{groupId}/admins/{userId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<GroupResponseDTO> addAdmin(
            @PathVariable Long groupId,
            @PathVariable Long userId,
            Authentication authentication
    ) {
        Long creatorId = securityUtils.getCurrentUserId(authentication);
        return ResponseEntity.ok(groupService.addAdmin(groupId, userId, creatorId));
    }

    // ── DELETE /groups/{groupId}/admins/{userId} ──────────────────
    @DeleteMapping("/{groupId}/admins/{userId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<GroupResponseDTO> removeAdmin(
            @PathVariable Long groupId,
            @PathVariable Long userId,
            Authentication authentication
    ) {
        Long creatorId = securityUtils.getCurrentUserId(authentication);
        return ResponseEntity.ok(groupService.removeAdmin(groupId, userId, creatorId));
    }

    // ── DELETE /groups/{groupId} ──────────────────────────────────
    @DeleteMapping("/{groupId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteGroup(
            @PathVariable Long groupId,
            Authentication authentication
    ) {
        Long creatorId = securityUtils.getCurrentUserId(authentication);
        groupService.deleteGroup(groupId, creatorId);
        return ResponseEntity.noContent().build();
    }

    /**
     * GET /groups/public - Récupère tous les groupes publics
     */
    @GetMapping("/public")
    public List<GroupResponseDTO> getPublicGroups() {
        return groupService.getPublicGroups();
    }

    /**
     * GET /groups/search?keyword=xxx - Recherche des groupes
     */
    @GetMapping("/search")
    public List<GroupResponseDTO> searchGroups(@RequestParam String keyword) {
        return groupService.searchGroups(keyword);
    }

    /**
     * POST /groups/{groupId}/join-requests - Demander à rejoindre un groupe
     * Le demandeur est l'utilisateur authentifié (on ne peut plus créer une demande au nom d'un autre).
     */
    @PostMapping("/{groupId}/join-requests")
    @PreAuthorize("isAuthenticated()")
    public JoinRequestDTO sendJoinRequest(
            @PathVariable Long groupId,
            Authentication authentication) {
        Long userId = securityUtils.getCurrentUserId(authentication);
        return groupService.sendJoinRequest(groupId, userId);
    }

    /**
     * GET /groups/{groupId}/join-requests/pending - Voir les demandes en attente (admin uniquement)
     */
    @GetMapping("/{groupId}/join-requests/pending")
    @PreAuthorize("isAuthenticated()")
    public List<JoinRequestDTO> getPendingJoinRequests(
            @PathVariable Long groupId,
            Authentication authentication) {
        Long adminId = securityUtils.getCurrentUserId(authentication);
        return groupService.getPendingJoinRequests(groupId, adminId);
    }

    /**
     * POST /groups/join-requests/{requestId}/accept - Accepter une demande (admin uniquement)
     */
    @PostMapping("/join-requests/{requestId}/accept")
    @PreAuthorize("isAuthenticated()")
    public JoinRequestDTO acceptJoinRequest(
            @PathVariable Long requestId,
            Authentication authentication) {
        Long adminId = securityUtils.getCurrentUserId(authentication);
        return groupService.acceptJoinRequest(requestId, adminId);
    }

    /**
     * POST /groups/join-requests/{requestId}/reject - Refuser une demande (admin uniquement)
     */
    @PostMapping("/join-requests/{requestId}/reject")
    @PreAuthorize("isAuthenticated()")
    public JoinRequestDTO rejectJoinRequest(
            @PathVariable Long requestId,
            Authentication authentication) {
        Long adminId = securityUtils.getCurrentUserId(authentication);
        return groupService.rejectJoinRequest(requestId, adminId);
    }

    /**
     * GET /groups/user/{userId}/pending-requests - Demandes en attente d'un utilisateur
     */
    @GetMapping("/user/{userId}/pending-requests")
    public List<JoinRequestDTO> getUserPendingRequests(@PathVariable Long userId) {
        return groupService.getUserPendingRequests(userId);
    }

    /**
     * PATCH /groups/{groupId}/visibility - Changer la visibilité (créateur uniquement)
     */
    @PatchMapping("/{groupId}/visibility")
    @PreAuthorize("isAuthenticated()")
    public GroupResponseDTO updateGroupVisibility(
            @PathVariable Long groupId,
            @RequestParam String visibility,
            Authentication authentication) {
        Long userId = securityUtils.getCurrentUserId(authentication);
        return groupService.updateGroupVisibility(groupId, visibility, userId);
    }
}
