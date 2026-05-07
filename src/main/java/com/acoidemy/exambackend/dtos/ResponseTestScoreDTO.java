package com.acoidemy.exambackend.dtos;

import lombok.Data;

import java.util.List;

@Data
public class ResponseTestScoreDTO {

    private String testId;
    private String userName;
    private String examSetName;
    private int score;
    private int numberOfQuestions;
    private int numberOfFailedQuestions;
    private int numberOfSucceededQuestions;

    private List<QuestionDTO> FailedQuestions;

}
