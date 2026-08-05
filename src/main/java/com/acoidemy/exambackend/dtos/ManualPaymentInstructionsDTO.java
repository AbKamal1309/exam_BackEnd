package com.acoidemy.exambackend.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ManualPaymentInstructionsDTO {
    private String bankRib;
    private String bankName;
    private String whatsappNumber;
    private double monthlyPriceMad;
    private double annualPriceMad;
}
