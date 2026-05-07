package com.acoidemy.exambackend.dtos;

import lombok.Data;

@Data
public class BestScoreDTO {
    private String examId;
    private String testId;
    private int bestScore;
    private String name;
}
