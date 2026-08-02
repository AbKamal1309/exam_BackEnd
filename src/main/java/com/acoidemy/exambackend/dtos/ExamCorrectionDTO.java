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
public class ExamCorrectionDTO {
    private String codeExam;
    private String examDescription;
    private int attemptsUsed;
    private int maxAttempts;
    // Basée sur la DERNIÈRE tentative de l'utilisateur (la plus pertinente à revoir).
    private List<QuestionCorrectionDTO> questions;
}
