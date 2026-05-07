package com.acoidemy.exambackend.entities;

import com.acoidemy.exambackend.enums.AnswerStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Entity
@Data @AllArgsConstructor @NoArgsConstructor
public class Answer {
    @Id
    private String codeAnswer;
    private String answerContent;
    private String description;
    @Enumerated(EnumType.STRING)
    private AnswerStatus answerStatus=AnswerStatus.WRONG;
    @ManyToOne
    private Question question;
    @OneToMany(mappedBy = "answer")
    private List<TestAnswer> testAnswer;

}
