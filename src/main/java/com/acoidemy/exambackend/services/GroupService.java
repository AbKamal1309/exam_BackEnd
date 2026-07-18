package com.acoidemy.exambackend.services;

import com.acoidemy.exambackend.dtos.GroupRequestDTO;
import com.acoidemy.exambackend.dtos.GroupResponseDTO;
import com.acoidemy.exambackend.dtos.JoinRequestDTO;
import com.acoidemy.exambackend.dtos.UserSummaryDTO;
import com.acoidemy.exambackend.entities.AppUser;
import com.acoidemy.exambackend.entities.Group;
import com.acoidemy.exambackend.entities.JoinRequest;
import com.acoidemy.exambackend.repositories.AppUserRepository;
import com.acoidemy.exambackend.repositories.ExamRepository;
import com.acoidemy.exambackend.repositories.GroupRepository;
import com.acoidemy.exambackend.repositories.JoinRequestRepository;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
@Builder
public class GroupService {

    private final GroupRepository groupRepository;
    private final JoinRequestRepository joinRequestRepository;
    private final ExamRepository examRepository;
    private final AppUserRepository userRepository;
    private final SocketService socketService;

    // ==================== CRÉATION ET LECTURE ====================

    /**
     * Créer un groupe
     */
    public GroupResponseDTO createGroup(GroupRequestDTO dto) {
        AppUser creator = findUserById(dto.getCreatorId());

        if (groupRepository.existsByGroupName(dto.getName())) {
            throw new RuntimeException("Un groupe avec ce nom existe déjà.");
        }

        Group group = Group.builder()
                .groupName(dto.getName())
                .groupDescription(dto.getDescription())
                .creator(creator)
                .visibility(dto.getVisibility() != null ? dto.getVisibility() : "PRIVATE") // Par défaut privé
                .dateCreated(new Date())
                .build();

        // Le créateur devient automatiquement membre et admin
        group.addAdmin(creator);
        group.addMember(creator);

        Group saved = groupRepository.save(group);
        return toGroupResponseDTO(saved);
    }

    /**
     * Obtenir un groupe par ID
     */
    @Transactional(readOnly = true)
    public GroupResponseDTO getGroup(Long groupId) {
        return toGroupResponseDTO(findGroupById(groupId));
    }

    /**
     * Lister tous les groupes
     */
    @Transactional(readOnly = true)
    public List<GroupResponseDTO> getAllGroups() {
        return groupRepository.findAll()
                .stream()
                .map(this::toGroupResponseDTO)
                .collect(Collectors.toList());
    }

    /**
     * Groupes créés par un utilisateur
     */
    @Transactional(readOnly = true)
    public List<GroupResponseDTO> getGroupsByCreator(Long creatorId) {
        return groupRepository.findByCreatorId(creatorId)
                .stream()
                .map(this::toGroupResponseDTO)
                .collect(Collectors.toList());
    }

    /**
     * Groupes d'un membre
     */
    @Transactional(readOnly = true)
    public List<GroupResponseDTO> getGroupsByMember(Long userId) {
        return groupRepository.findGroupsByMemberId(userId)
                .stream()
                .map(this::toGroupResponseDTO)
                .collect(Collectors.toList());
    }

    /**
     * Récupère tous les groupes publics
     */
    @Transactional(readOnly = true)
    public List<GroupResponseDTO> getPublicGroups() {
        return groupRepository.findByVisibility("PUBLIC")
                .stream()
                .map(this::toGroupResponseDTO)
                .collect(Collectors.toList());
    }

    /**
     * Recherche des groupes par nom ou description
     */
    @Transactional(readOnly = true)
    public List<GroupResponseDTO> searchGroups(String keyword) {
        return groupRepository.findByGroupNameContainingIgnoreCaseOrGroupDescriptionContainingIgnoreCase(keyword, keyword)
                .stream()
                .map(this::toGroupResponseDTO)
                .collect(Collectors.toList());
    }

    // ==================== GESTION DES MEMBRES ====================

    /**
     * Ajouter un membre (admin ou créateur uniquement)
     */
    public GroupResponseDTO addMember(Long groupId, Long userId, Long requesterId) {
        Group group = findGroupById(groupId);
        AppUser user = findUserById(userId);
        AppUser requester = findUserById(requesterId);

        // Vérifier si l'utilisateur est déjà membre
        if (group.isMember(user)) {
            throw new RuntimeException("Cet utilisateur est déjà membre du groupe.");
        }

        // Cas 1 : Le groupe est PUBLIC - n'importe qui peut rejoindre
        if ("PUBLIC".equals(group.getVisibility())) {
            group.addMember(user);
            return toGroupResponseDTO(groupRepository.save(group));
        }

        // Cas 2 : Groupe PRIVE - seul un admin ou le créateur peut ajouter
        if (!group.isCreator(requester) && !group.isAdmin(requester)) {
            throw new RuntimeException("Seul le créateur ou un admin peut ajouter un membre.");
        }

        group.addMember(user);
        return toGroupResponseDTO(groupRepository.save(group));
    }

    /**
     * Supprimer un membre (admin ou créateur uniquement)
     */
    public GroupResponseDTO removeMember(Long groupId, Long userId, Long requesterId) {
        Group group = findGroupById(groupId);
        AppUser user = findUserById(userId);
        AppUser requester = findUserById(requesterId);

        if (!group.isCreator(requester) && !group.isAdmin(requester)) {
            throw new RuntimeException("Seul le créateur ou un admin peut supprimer un membre.");
        }
        if (group.isCreator(user)) {
            throw new RuntimeException("Impossible de supprimer le créateur du groupe.");
        }

        group.removeMember(user);
        group.removeAdmin(user);
        return toGroupResponseDTO(groupRepository.save(group));
    }

    // ==================== GESTION DES ADMINS ====================

    /**
     * Ajouter un admin (uniquement le créateur)
     */
    public GroupResponseDTO addAdmin(Long groupId, Long userId, Long creatorId) {
        Group group = findGroupById(groupId);
        AppUser user = findUserById(userId);
        AppUser creator = findUserById(creatorId);

        if (!group.isCreator(creator)) {
            throw new RuntimeException("Seul le créateur peut désigner un admin.");
        }
        if (group.isAdmin(user)) {
            throw new RuntimeException("Cet utilisateur est déjà admin du groupe.");
        }

        group.addAdmin(user);
        group.addMember(user); // S'assurer qu'il est aussi membre
        return toGroupResponseDTO(groupRepository.save(group));
    }

    /**
     * Supprimer un admin (uniquement le créateur)
     */
    public GroupResponseDTO removeAdmin(Long groupId, Long userId, Long creatorId) {
        Group group = findGroupById(groupId);
        AppUser user = findUserById(userId);
        AppUser creator = findUserById(creatorId);

        if (!group.isCreator(creator)) {
            throw new RuntimeException("Seul le créateur peut retirer un admin.");
        }
        if (group.isCreator(user)) {
            throw new RuntimeException("Impossible de retirer le créateur de son rôle d'admin.");
        }

        group.removeAdmin(user);
        return toGroupResponseDTO(groupRepository.save(group));
    }

    // ==================== GESTION DES DEMANDES D'ADHÉSION ====================

    /**
     * Envoyer une demande pour rejoindre un groupe
     */
    public JoinRequestDTO sendJoinRequest(Long groupId, Long userId) {
        Group group = findGroupById(groupId);
        AppUser user = findUserById(userId);

        // Vérifier si déjà membre
        if (group.isMember(user)) {
            throw new RuntimeException("Vous êtes déjà membre de ce groupe");
        }

        // Vérifier si une demande est déjà en attente
        if (joinRequestRepository.existsByGroupIdAndUserIdAndStatus(groupId, userId, "PENDING")) {
            throw new RuntimeException("Une demande est déjà en attente");
        }

        // Pour les groupes publics, ajouter directement
        if ("PUBLIC".equals(group.getVisibility())) {
            group.addMember(user);
            groupRepository.save(group);
            return JoinRequestDTO.builder()
                    .status("ACCEPTED")
                    .groupId(groupId)
                    .userId(userId)
                    .groupName(group.getGroupName())
                    .userName(user.getName())
                    .userEmail(user.getEmail())
                    .build();
        }

        // Pour les groupes privés, créer une demande
        JoinRequest request = JoinRequest.builder()
                .group(group)
                .user(user)
                .status("PENDING")
                .requestDate(LocalDateTime.now())
                .build();

        JoinRequest saved = joinRequestRepository.save(request);
        // Après avoir créé la demande, envoyer la notification
        socketService.sendJoinRequestNotification(groupId, group.getGroupName(), user.getName());

        return toJoinRequestDTO(saved);
    }

    /**
     * Récupère les demandes en attente pour un groupe (admin uniquement)
     */
    @Transactional(readOnly = true)
    public List<JoinRequestDTO> getPendingJoinRequests(Long groupId, Long adminId) {
        Group group = findGroupById(groupId);

        // Vérifier que l'utilisateur est admin ou créateur
        boolean isAuthorized = group.isCreatorId(adminId) || group.isAdminId(adminId);

        if (!isAuthorized) {
            throw new RuntimeException("Seuls les administrateurs peuvent voir les demandes");
        }

        return joinRequestRepository.findByGroupIdAndStatus(groupId, "PENDING")
                .stream()
                .map(this::toJoinRequestDTO)
                .collect(Collectors.toList());
    }

    /**
     * Accepte une demande d'adhésion
     */
    public JoinRequestDTO acceptJoinRequest(Long requestId, Long adminId) {
        JoinRequest request = findJoinRequestById(requestId);
        Group group = request.getGroup();

        // Vérifier que l'admin a le droit
        boolean isAuthorized = group.isCreatorId(adminId) || group.isAdminId(adminId);

        if (!isAuthorized) {
            throw new RuntimeException("Seuls les administrateurs peuvent accepter les demandes");
        }

        // Ajouter l'utilisateur aux membres
        group.addMember(request.getUser());
        request.setStatus("ACCEPTED");

        groupRepository.save(group);
        joinRequestRepository.save(request);

        return toJoinRequestDTO(request);
    }

    /**
     * Refuse une demande d'adhésion
     */
    public JoinRequestDTO rejectJoinRequest(Long requestId, Long adminId) {
        JoinRequest request = findJoinRequestById(requestId);
        Group group = request.getGroup();

        // Vérifier que l'admin a le droit
        boolean isAuthorized = group.isCreatorId(adminId) || group.isAdminId(adminId);

        if (!isAuthorized) {
            throw new RuntimeException("Seuls les administrateurs peuvent refuser les demandes");
        }

        request.setStatus("REJECTED");
        joinRequestRepository.save(request);

        return toJoinRequestDTO(request);
    }

    /**
     * Récupère les demandes en attente d'un utilisateur
     */
    @Transactional(readOnly = true)
    public List<JoinRequestDTO> getUserPendingRequests(Long userId) {
        return joinRequestRepository.findByUserIdAndStatus(userId, "PENDING")
                .stream()
                .map(this::toJoinRequestDTO)
                .collect(Collectors.toList());
    }

    // ==================== GESTION DE LA VISIBILITÉ ====================

    /**
     * Change la visibilité d'un groupe (uniquement le créateur)
     */
    public GroupResponseDTO updateGroupVisibility(Long groupId, String visibility, Long userId) {
        Group group = findGroupById(groupId);

        if (!group.isCreatorId(userId)) {
            throw new RuntimeException("Seul le créateur peut modifier la visibilité du groupe");
        }

        group.setVisibility(visibility);
        groupRepository.save(group);

        return toGroupResponseDTO(group);
    }

    // ==================== PARTAGE D'EXAMENS ====================

    /**
     * Partage un examen avec un groupe (admin uniquement)
     */
    public GroupResponseDTO shareExamWithGroup(String examCode, Long groupId, Long adminId) {
        Group group = findGroupById(groupId);
        var exam = examRepository.findById(examCode)
                .orElseThrow(() -> new RuntimeException("Examen non trouvé"));

        boolean isAuthorized = group.isCreatorId(adminId) || group.isAdminId(adminId);

        if (!isAuthorized) {
            throw new RuntimeException("Seul un admin peut partager un examen dans ce groupe");
        }

        group.addSharedExam(exam);
        return toGroupResponseDTO(groupRepository.save(group));
    }

    /**
     * Retire un examen partagé d'un groupe (admin uniquement)
     */
    public GroupResponseDTO unshareExamFromGroup(String examCode, Long groupId, Long adminId) {
        Group group = findGroupById(groupId);
        var exam = examRepository.findById(examCode)
                .orElseThrow(() -> new RuntimeException("Examen non trouvé"));

        boolean isAuthorized = group.isCreatorId(adminId) || group.isAdminId(adminId);

        if (!isAuthorized) {
            throw new RuntimeException("Seul un admin peut retirer un examen partagé");
        }

        group.removeSharedExam(exam);
        return toGroupResponseDTO(groupRepository.save(group));
    }

    /**
     * Récupère les examens partagés d'un groupe
     */
    @Transactional(readOnly = true)
    public List<?> getSharedExamsForGroup(Long groupId, Long userId) {
        Group group = findGroupById(groupId);

        if (!group.isMemberId(userId)) {
            throw new RuntimeException("Vous devez être membre pour voir les examens partagés");
        }

        return group.getSharedExams();
    }

    // ==================== SUPPRESSION ====================

    /**
     * Supprimer un groupe (uniquement le créateur)
     */
    public void deleteGroup(Long groupId, Long creatorId) {
        Group group = findGroupById(groupId);

        if (!group.isCreatorId(creatorId)) {
            throw new RuntimeException("Seul le créateur peut supprimer le groupe.");
        }

        groupRepository.delete(group);
    }

    /**
     * Supprimer un groupe en tant qu'administrateur de la plateforme, quel qu'en soit le créateur
     * (modération). Réservé aux endpoints protégés par @PreAuthorize("hasRole('ADMIN')").
     */
    public void adminDeleteGroup(Long groupId) {
        Group group = findGroupById(groupId);
        log.info("[ADMIN] Suppression du groupe {} ({})", groupId, group.getGroupName());
        groupRepository.delete(group);
    }

    // ==================== METHODES PRIVÉES ====================

    private Group findGroupById(Long id) {
        return groupRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Groupe introuvable avec l'ID : " + id));
    }

    private AppUser findUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable avec l'ID : " + id));
    }

    private JoinRequest findJoinRequestById(Long id) {
        return joinRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Demande introuvable avec l'ID : " + id));
    }

    // ==================== MAPPING ====================

    private GroupResponseDTO toGroupResponseDTO(Group group) {
        return GroupResponseDTO.builder()
                .id(group.getId())
                .name(group.getGroupName())
                .description(group.getGroupDescription())
                .visibility(group.getVisibility() != null ? group.getVisibility() : "PRIVATE")
                .createdAt(group.getCreatedAt())
                .creatorId(group.getCreator().getId())
                .creatorName(group.getCreator().getName())
                .membersCount(group.getMembers() != null ? group.getMembers().size() : 0)
                .adminsCount(group.getAdmins() != null ? group.getAdmins().size() : 0)
                .members(group.getMembers().stream()
                        .map(this::toUserSummaryDTO)
                        .collect(Collectors.toSet()))
                .admins(group.getAdmins().stream()
                        .map(this::toUserSummaryDTO)
                        .collect(Collectors.toSet()))
                .build();
    }

    private UserSummaryDTO toUserSummaryDTO(AppUser user) {
        return UserSummaryDTO.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .build();
    }

    private JoinRequestDTO toJoinRequestDTO(JoinRequest request) {
        return JoinRequestDTO.builder()
                .id(request.getId())
                .groupId(request.getGroup().getId())
                .groupName(request.getGroup().getGroupName())
                .userId(request.getUser().getId())
                .userName(request.getUser().getName())
                .userEmail(request.getUser().getEmail())
                .status(request.getStatus())
                .requestDate(request.getRequestDate())
                .build();
    }
}