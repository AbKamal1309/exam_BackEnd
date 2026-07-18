package com.acoidemy.exambackend.entities;

import com.acoidemy.exambackend.enums.AnswerStatus;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = {"testExam", "question", "chosenAnswer"})
public class TestAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    // ── Test auquel appartient cette réponse (Many-to-One) ────────
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "test_exam_id", nullable = false)
    private TestExam testExam;

    // ── Question posée (Many-to-One) ──────────────────────────────
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    // ── Réponse choisie par l'utilisateur (Many-to-One) ───────────
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chosen_answer_id", nullable = true)
    private Answer chosenAnswer;

    // ── Cette réponse est-elle correcte ? ─────────────────────────
    @Column(nullable = false)
    private boolean correct = false;

    // ── Calculer si la réponse est correcte ───────────────────────
    public void evaluate() {
        if (this.chosenAnswer != null) {
            this.correct = "CORRECT".equals(this.chosenAnswer.getAnswerStatus().name());
        }
    }

}