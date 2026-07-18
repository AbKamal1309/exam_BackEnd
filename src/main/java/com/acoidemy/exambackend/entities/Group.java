package com.acoidemy.exambackend.entities;

import com.acoidemy.exambackend.enums.GroupStatus;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "exam_groups")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = {"creator", "members", "admins", "sharedExams", "joinRequests"})
public class Group {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(nullable = false, unique = true)
    private String groupId;

    @Column(nullable = false)
    private String groupName;

    private String groupDescription;

    private String visibility; // "PUBLIC" ou "PRIVATE"

    @Column(nullable = false)
    private Date dateCreated;

    @Enumerated(EnumType.STRING)
    private GroupStatus groupStatus;

    // ── Créateur du groupe (Many-to-One) ──────────────────────────
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id", nullable = false)
    private AppUser creator;

    // ── Membres du groupe (Many-to-Many) ──────────────────────────
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "group_members",
            joinColumns        = @JoinColumn(name = "group_id",  referencedColumnName = "id"),
            inverseJoinColumns = @JoinColumn(name = "user_id",   referencedColumnName = "id")
    )
    @Builder.Default
    private List<AppUser> members = new ArrayList<>();

    // ── Admins du groupe (Many-to-Many) ───────────────────────────
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "group_admins",
            joinColumns        = @JoinColumn(name = "group_id",  referencedColumnName = "id"),
            inverseJoinColumns = @JoinColumn(name = "user_id",   referencedColumnName = "id")
    )
    @Builder.Default
    private List<AppUser> admins = new ArrayList<>();

    // ── Examens partagés avec ce groupe ───────────────────────────
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "group_shared_exams",
            joinColumns        = @JoinColumn(name = "group_id", referencedColumnName = "id"),
            inverseJoinColumns = @JoinColumn(name = "exam_code", referencedColumnName = "codeExam")
    )
    @Builder.Default
    private List<Exam> sharedExams = new ArrayList<>();

    // ── Demandes d'adhésion ───────────────────────────────────────
    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<JoinRequest> joinRequests = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        if (this.dateCreated == null) {
            this.dateCreated = new Date();
        }
        if (this.groupStatus == null) {
            this.groupStatus = GroupStatus.ACTIVE;
        }
        if (this.visibility == null) {
            this.visibility = "PRIVATE";
        }
        // Générer un ID unique automatiquement
        if (this.groupId == null) {
            this.groupId = "GRP-" + java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        }
    }

    // ==================== METHODES UTILITAIRES ====================

    public void addMember(AppUser user) {
        if (!this.members.contains(user)) {
            this.members.add(user);
        }
    }

    public void removeMember(AppUser user) {
        this.members.remove(user);
        this.admins.remove(user);
    }

    public void addAdmin(AppUser user) {
        addMember(user);
        if (!this.admins.contains(user)) {
            this.admins.add(user);
        }
    }

    public void removeAdmin(AppUser user) {
        this.admins.remove(user);
    }

    public void addSharedExam(Exam exam) {
        if (this.sharedExams == null) {
            this.sharedExams = new ArrayList<>();
        }
        if (!this.sharedExams.contains(exam)) {
            this.sharedExams.add(exam);
        }
    }

    public void removeSharedExam(Exam exam) {
        if (this.sharedExams != null) {
            this.sharedExams.remove(exam);
        }
    }

    public void addJoinRequest(JoinRequest request) {
        if (this.joinRequests == null) {
            this.joinRequests = new ArrayList<>();
        }
        if (!this.joinRequests.contains(request)) {
            this.joinRequests.add(request);
            request.setGroup(this);
        }
    }

    // ==================== METHODES DE VERIFICATION ====================

    public boolean isCreator(AppUser user) {
        return this.creator != null && user != null
                && this.creator.getId().equals(user.getId());
    }

    public boolean isCreatorId(Long userId) {
        return this.creator != null && userId != null
                && this.creator.getId().equals(userId);
    }

    public boolean isAdmin(AppUser user) {
        return this.admins.stream()
                .anyMatch(a -> a.getId().equals(user.getId()));
    }

    public boolean isAdminId(Long userId) {
        return this.admins.stream()
                .anyMatch(a -> a.getId().equals(userId));
    }

    public boolean isMember(AppUser user) {
        return this.members.stream()
                .anyMatch(m -> m.getId().equals(user.getId()));
    }

    public void shareExam(Exam exam, AppUser requestingAdmin) {
        if (!isAdmin(requestingAdmin)) {
            throw new RuntimeException("Seul un admin peut partager un examen.");
        }
        boolean isOwner = exam.getAppUser() != null &&
                exam.getAppUser().getId().equals(requestingAdmin.getId());
        boolean isPublic = exam.isPublic();

        if (!isOwner && !isPublic) {
            throw new RuntimeException(
                    "Vous ne pouvez partager qu'un examen PUBLIC ou un examen que vous avez créé.");
        }
        if (this.sharedExams == null) this.sharedExams = new ArrayList<>();
        if (!this.sharedExams.contains(exam)) this.sharedExams.add(exam);
    }

    public void unshareExam(Exam exam, AppUser requestingAdmin) {
        if (!isAdmin(requestingAdmin)) {
            throw new RuntimeException("Seul un admin peut retirer un examen partagé.");
        }
        if (this.sharedExams != null) this.sharedExams.remove(exam);
    }

    public boolean isMemberId(Long userId) {
        return this.members.stream()
                .anyMatch(m -> m.getId().equals(userId));
    }

    public boolean isPublic() {
        return "PUBLIC".equals(this.visibility);
    }

    // ==================== GETTERS AVEC VALEURS PAR DÉFAUT ====================

    public String getName() {
        return this.groupName;
    }

    public String getGroupDescription() {
        return this.groupDescription;
    }

    public Date getCreatedAt() {
        return this.dateCreated;
    }

    public String getVisibility() {
        return this.visibility != null ? this.visibility : "PRIVATE";
    }

    public List<AppUser> getMembers() {
        return this.members != null ? this.members : new ArrayList<>();
    }

    public List<AppUser> getAdmins() {
        return this.admins != null ? this.admins : new ArrayList<>();
    }

    public List<Exam> getSharedExams() {
        return this.sharedExams != null ? this.sharedExams : new ArrayList<>();
    }

    public List<JoinRequest> getJoinRequests() {
        return this.joinRequests != null ? this.joinRequests : new ArrayList<>();
    }
}