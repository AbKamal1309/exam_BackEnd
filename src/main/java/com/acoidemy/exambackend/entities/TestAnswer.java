package com.acoidemy.exambackend.entities;

import com.acoidemy.exambackend.enums.AnswerStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TestAnswer {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long testAnswerId;



    @ManyToOne
    private TestExam testExam;
    @Enumerated(EnumType.STRING)
    private AnswerStatus answerTestStatus=AnswerStatus.CORRECT;

    @ManyToOne
    private Question   question;
    @ManyToOne
    private Answer answer;
}
