package com.acoidemy.exambackend.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data @NoArgsConstructor
@AllArgsConstructor
@Builder
public class TestResultDTO {

    private String  testId;
    private String  examId;
    private String  userNameTest;
    private String  userNameExamSetter;
    private int     score;            // nombre de bonnes réponses
    private double  scorePercentage;  // ← NOUVEAU : score en %
    private int     totalQuestions;
    private int     correctAnswers;
    private int     wrongAnswers;
    private Date datePassed;       // ← NOUVEAU : date du passage
}
