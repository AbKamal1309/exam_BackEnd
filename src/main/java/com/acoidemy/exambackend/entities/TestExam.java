package com.acoidemy.exambackend.entities;

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
@ToString(exclude = {"appUser", "exam", "testAnswers"})
public class TestExam {

    @Id
    @EqualsAndHashCode.Include
    private String codeTest;

    // ── Date du passage du test ───────────────────────────────────
    @Column(nullable = false)
    private Date datePassed;

    // ── Score du test ───────────────────────────────────
    // Nombre de bonnes réponses sur le total des questions
    @Column(nullable = false)
    private int score = 0;

    // ── Score en pourcentage ────────────────────────────
    @Column(nullable = false)
    private double scorePercentage = 0.0;

    // ── Nombre total de questions au moment du test ───────────────
    private int totalQuestions;

    // ── Nombre de bonnes / mauvaises réponses ────────────────────
    private int correctAnswers;
    private int wrongAnswers;

    // ── Candidat qui passe le test (Many-to-One) ──────────────────
    // Un AppUser peut passer PLUSIEURS tests (même examen)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "app_user_id", nullable = false)
    private AppUser appUser;

    // ── Examen testé (Many-to-One) ────────────────────────────────
    // Un Exam peut avoir PLUSIEURS tests passés dessus
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exam_id", nullable = false)
    private Exam exam;

    // ── Réponses soumises dans ce test ───────────────────────────
    @OneToMany(mappedBy = "testExam", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<TestAnswer> testAnswers = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        if (this.datePassed == null) this.datePassed = new Date();
    }

    // ── Calculer le score automatiquement ─────────────────────────
    public void calculateScore() {
        if (this.testAnswers == null || this.testAnswers.isEmpty()) return;
        this.totalQuestions   = this.testAnswers.size();
        this.correctAnswers   = (int) this.testAnswers.stream()
                .filter(TestAnswer::isCorrect).count();
        this.wrongAnswers     = this.totalQuestions - this.correctAnswers;
        this.score            = this.correctAnswers;
        this.scorePercentage  = this.totalQuestions > 0
                ? ((double) this.correctAnswers / this.totalQuestions) * 100 : 0;
    }

}