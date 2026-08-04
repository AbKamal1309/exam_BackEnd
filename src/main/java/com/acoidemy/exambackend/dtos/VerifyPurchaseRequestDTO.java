package com.acoidemy.exambackend.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VerifyPurchaseRequestDTO {
    private String productId;      // "premium_monthly" | "premium_annual"
    private String purchaseToken;  // fourni par la Play Billing Library côté mobile
}
