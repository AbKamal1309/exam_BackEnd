package com.acoidemy.exambackend.entities;

import com.acoidemy.exambackend.enums.ExamStatus;
import com.acoidemy.exambackend.enums.ExamVisibility;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = {"appUser", "originalExam", "questions", "testExams", "sharedWithGroups"})
public class Exam {

    @Id
    @EqualsAndHashCode.Include
    private String codeExam;

    private Date dateCreation;
    private int numberOfQuestions;

    @Enumerated(EnumType.STRING)
    private ExamStatus status; // CREATED, ACTIVATED, SUSPENDED

    private String description;

    // ── Visibilité de l'examen ──────────────────────────
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExamVisibility visibility = ExamVisibility.PRIVATE;


    // ── Créateur de l'examen (Many-to-One) ───────────────────────
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "app_user_id")
    private AppUser appUser;

    // ── Examen original dont celui-ci est une copie ─────
    // Si un utilisateur copie un examen PUBLIC → on garde la référence
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "original_exam_id", nullable = true)
    private Exam originalExam;

    @Column(name = "duration_minutes")
    private Integer durationMinutes; // null = pas de limite de temps


    // ── Questions de l'examen ─────────────────────────────────────
    @OneToMany(mappedBy = "exam", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Question> questions = new ArrayList<>();


    // ── Tests passés sur cet examen ───────────────────────────────
    @OneToMany(mappedBy = "exam", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<TestExam> testExams = new ArrayList<>();

    // ── Groupes avec lesquels cet examen est partagé ───
    @ManyToMany(mappedBy = "sharedExams", fetch = FetchType.LAZY)
    private List<Group> sharedWithGroups = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        if (this.dateCreation == null) this.dateCreation = new Date();
        if (this.visibility == null) this.visibility = ExamVisibility.PRIVATE;
    }

    // ── Méthode utilitaire : est-ce un examen copié ? ─────────────
    public boolean isCopy() {
        return this.originalExam != null;
    }

    public boolean isPublic() {
        return ExamVisibility.PUBLIC.equals(this.visibility);
    }


}