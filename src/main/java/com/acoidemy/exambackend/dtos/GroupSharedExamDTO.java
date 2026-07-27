package com.acoidemy.exambackend.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Un examen partagé dans un groupe dont l'utilisateur est membre, avec le
 * contexte du groupe (nom + id) — affiché sur le Dashboard mobile avec la
 * mention "Partagé dans le groupe <nom>". Si un même examen est partagé dans
 * plusieurs groupes de l'utilisateur, il apparaît une fois par groupe (une
 * mention différente à chaque fois), plutôt que d'essayer de le fusionner.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GroupSharedExamDTO {
    private ExamDTO examDTO;
    private Long groupId;
    private String groupName;
}