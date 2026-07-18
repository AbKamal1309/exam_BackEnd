package com.acoidemy.exambackend.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.Set;

// ── DTO Réponse groupe ────────────────────────────────────────────
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GroupResponseDTO {

    private Long          id;
    private String        name;
    private String        description;
    private Date createdAt;

    // Infos du créateur
    private Long   creatorId;
    private String creatorName;

    private String visibility;

    // Nombre de membres et admins
    private int membersCount;
    private int adminsCount;

    // Listes des membres et admins (IDs + noms)
    private Set<UserSummaryDTO> members;
    private Set<UserSummaryDTO> admins;

}
