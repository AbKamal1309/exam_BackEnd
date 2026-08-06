package com.acoidemy.exambackend.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminUserSummaryDTO {
    private Long id;
    private String name;
    private String email;
    private boolean isAdmin;
    private boolean isPremium;
    private LocalDateTime premiumUntil;
}
