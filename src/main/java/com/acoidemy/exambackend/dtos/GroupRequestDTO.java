package com.acoidemy.exambackend.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// ── DTO Création d'un groupe ──────────────────────────────────────
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GroupRequestDTO {

    private String name;
    private String description;

    // ID du créateur (fourni dans la requête)
    private Long creatorId;
    private String visibility;
}
