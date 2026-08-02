package com.acoidemy.exambackend.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnswerCorrectionDTO {
    private String answerContent;
    private boolean actuallyCorrect; // vraie réponse (visible uniquement ici, jamais avant la correction)
    private boolean userSelected;    // l'utilisateur a-t-il coché cette réponse ?
}
