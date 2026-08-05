package com.acoidemy.exambackend.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ManualPremiumRequestCreateDTO {
    private String plan;             // "MONTHLY" | "ANNUAL"
    private String paymentReference; // optionnel, libre
}
