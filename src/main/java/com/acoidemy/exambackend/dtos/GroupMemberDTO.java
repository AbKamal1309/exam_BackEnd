package com.acoidemy.exambackend.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// ── DTO pour ajouter/supprimer un membre ou admin ─────────────────
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GroupMemberDTO {

    private Long groupId;
    private Long userId;
    private Long requesterId; // ID de celui qui fait la demande (vérif créateur)
}
