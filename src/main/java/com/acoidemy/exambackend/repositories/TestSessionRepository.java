package com.acoidemy.exambackend.repositories;

import com.acoidemy.exambackend.entities.TestSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TestSessionRepository extends JpaRepository<TestSession, Long> {
    Optional<TestSession> findByAppUserIdAndExamCodeExamAndSubmittedAtIsNull(Long userId, String codeExam);
}