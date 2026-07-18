package com.acoidemy.exambackend.repositories;

import com.acoidemy.exambackend.entities.Exam;
import com.acoidemy.exambackend.enums.ExamVisibility;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ExamRepository extends JpaRepository<Exam,String> {
    Exam findByCodeExam(String codeExam);

    // Examens d'un utilisateur
    List<Exam> findByAppUserId(Long userId);

    // NOUVEAU : Examens publics
    List<Exam> findByVisibility(ExamVisibility visibility);

    // NOUVEAU : Examens publics d'un utilisateur
    List<Exam> findByAppUserIdAndVisibility(Long userId, ExamVisibility visibility);


    // NOUVEAU : Copies d'un examen original
    List<Exam> findByOriginalExamCodeExam(String originalCode);

    // NOUVEAU : Examens partagés avec un groupe
    @Query("SELECT e FROM Exam e JOIN e.sharedWithGroups g WHERE g.id = :groupId")
    List<Exam> findSharedExamsByGroupId(@Param("groupId") Long groupId);



}
