package com.acoidemy.exambackend.repositories;

import com.acoidemy.exambackend.entities.TestExam;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TestExamRepository extends JpaRepository<TestExam,String> {

    // Tous les tests d'un utilisateur
    List<TestExam> findByAppUserId(Long userId);

    // Tous les tests pour un examen
    List<TestExam> findByExamCodeExam(String codeExam);

    // NOUVEAU : Tous les tests d'un utilisateur pour UN examen spécifique
    // → Permet l'historique des passages multiples
    List<TestExam> findByAppUserIdAndExamCodeExam(Long userId, String codeExam);

    // Meilleur score d'un utilisateur pour un examen
    // → Utile pour le dashboard
    TestExam findTopByAppUserIdAndExamCodeExamOrderByScoreDesc(Long userId, String codeExam);

    long countByAppUserIdAndExamCodeExam(Long userId, String examCode);


}
