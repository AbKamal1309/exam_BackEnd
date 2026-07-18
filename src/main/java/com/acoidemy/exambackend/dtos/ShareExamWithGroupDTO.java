package com.acoidemy.exambackend.dtos;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// ── DTO pour partager un examen avec un groupe ────────────────────
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShareExamWithGroupDTO {

    private String examCode;   // Code de l'examen à partager
    private Long   groupId;    // ID du groupe
    private Long   adminId;    // ID de l'admin qui partage

}
