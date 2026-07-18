package com.acoidemy.exambackend.dtos;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// ── DTO pour copier un examen public ─────────────────────────────
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CopyExamDTO {

    private String examCode;   // Code de l'examen public à copier
    private Long   userId;     // ID de l'utilisateur qui copie
    private String newDescription; // Description optionnelle pour la copie



}
