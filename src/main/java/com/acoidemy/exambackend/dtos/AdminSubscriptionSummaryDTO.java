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
public class AdminSubscriptionSummaryDTO {
    private Long id;
    private String userName;
    private String userEmail;
    private String plan;      // "MONTHLY" | "ANNUAL"
    private String provider;  // "GOOGLE_PLAY" | "MANUAL"
    private String status;
    private LocalDateTime expiryDate;
}
