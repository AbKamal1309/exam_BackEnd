package com.acoidemy.exambackend.dtos;

import lombok.Data;

import java.util.List;
@Data
public class ScoreDTO {
    private String testId;
    private int score;

    private List<QuestionDTO> FailedQuestions;
}
