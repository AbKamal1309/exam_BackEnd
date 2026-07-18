package com.acoidemy.exambackend.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;
@Data
@AllArgsConstructor
public class ScoreDTO {
    private String testId;
    private int score;
    private int correctAnswers;
    private int wrongAnswers;
    private double scorePercentage = 0.0;

    private List<QuestionDTO> FailedQuestions;

    public ScoreDTO() {

    }
}
