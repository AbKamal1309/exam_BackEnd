package com.acoidemy.exambackend.dtos;

import lombok.Data;

@Data
public class AdminStatsDTO {
    private long totalUsers;
    private long totalExams;
    private long totalGroups;
    private long totalQuestions;
    private long totalTestsPassed;
}
