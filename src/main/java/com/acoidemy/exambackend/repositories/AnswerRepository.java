package com.acoidemy.exambackend.repositories;

import com.acoidemy.exambackend.entities.Answer;
import com.acoidemy.exambackend.entities.Question;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AnswerRepository extends JpaRepository<Answer,String> {
    List<Answer> findByQuestion(Question question);

}
