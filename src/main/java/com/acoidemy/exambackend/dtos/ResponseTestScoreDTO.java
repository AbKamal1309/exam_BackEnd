package com.acoidemy.exambackend.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResponseTestScoreDTO {

    private String  testId;
    private String  userName;
    private String  examSetName;
    private int     score;
    private double  scorePercentage;       // ← NOUVEAU
    private int     numberOfQuestions;
    private int     numberOfFailedQuestions;
    private int     numberOfSucceededQuestions;
    private List<QuestionDTO> failedQuestions;
    // Tous les tests passés par l'utilisateur sur cet examen
    private List<TestResultDTO> allUserTests; // ← NOUVEAU

}
