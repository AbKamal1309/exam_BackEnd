package com.acoidemy.exambackend.dtos;

import lombok.Data;

@Data
public class GenerateQuestionsRequestDTO {
    private String subject;                        // ex: "Dérivées et primitives"
    private String level;                           // ex: "Terminale S", "Licence 1"
    private Integer numberOfQuestions = 5;
    private String language = "fr";                 // "fr" ou "ar"
    private Boolean allowMultipleCorrectAnswers = true;
    private String difficulty = "MOYEN";             // "FACILE", "MOYEN", "AVANCE"
}
