package com.acoidemy.exambackend.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
@Data @NoArgsConstructor
@AllArgsConstructor
@Builder
public class TestExamDTO {

    private String  userRequestName;
    private String  codeExam;
    private String  userNameExamSetter;
    private ExamDTO examDTO;
    private String testStartTime; // ISO-8601, heure serveur de démarrage du test (persistée)
    private String serverTime;    // ISO-8601, heure serveur au moment de la réponse
    // Historique des tests déjà passés par l'utilisateur sur cet examen
    private List<TestResultDTO> previousTests;
}
