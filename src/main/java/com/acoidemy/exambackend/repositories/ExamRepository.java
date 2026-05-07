package com.acoidemy.exambackend.repositories;

import com.acoidemy.exambackend.entities.Exam;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExamRepository extends JpaRepository<Exam,String> {
    Exam findByCodeExam(String codeExam);


}
