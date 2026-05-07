package com.acoidemy.exambackend.repositories;

import com.acoidemy.exambackend.entities.Question;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuestionRepository extends JpaRepository<Question,String> {
List<Question> findByExamCodeExam(String codeExam);
Page<Question> findByExamCodeExamOrderByDateCreationDesc(String codeExam,Pageable pageable);

}
