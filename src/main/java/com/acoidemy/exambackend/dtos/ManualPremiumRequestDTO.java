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
public class ManualPremiumRequestDTO {
    private Long id;
    private Long userId;
    private String userName;
    private String userEmail;
    private String plan;
    private String paymentReference;
    private String status;
    private LocalDateTime createdAt;
}
