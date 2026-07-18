package com.acoidemy.exambackend.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// ── DTO résumé utilisateur (pour éviter la récursion) ────────────
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSummaryDTO {

    private Long   id;
    private String name;
    private String email;
}
