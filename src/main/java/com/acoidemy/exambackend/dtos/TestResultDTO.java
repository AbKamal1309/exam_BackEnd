package com.acoidemy.exambackend.dtos;

import lombok.Data;

@Data
public class TestResultDTO {

    private String testId;
    private String examId;
    private String userNameTest;

    private String userNameExamSetter;

    private int score;
}
