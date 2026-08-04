package com.acoidemy.exambackend.web;

import com.acoidemy.exambackend.dtos.SubscriptionStatusDTO;
import com.acoidemy.exambackend.dtos.VerifyPurchaseRequestDTO;
import com.acoidemy.exambackend.services.BillingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/billing")
@RequiredArgsConstructor
@Slf4j
public class BillingController {

    private final BillingService billingService;

    @PostMapping("/verify-purchase")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<SubscriptionStatusDTO> verifyPurchase(
            @RequestBody VerifyPurchaseRequestDTO request,
            Authentication authentication) {
        try {
            return ResponseEntity.ok(billingService.verifyPurchase(request, authentication));
        } catch (Exception e) {
            log.error("Échec de vérification d'achat Google Play", e);
            throw new RuntimeException("Impossible de vérifier cet achat pour le moment. Réessayez dans un instant.");
        }
    }

    @GetMapping("/status")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<SubscriptionStatusDTO> getStatus(Authentication authentication) {
        return ResponseEntity.ok(billingService.getStatus(authentication));
    }
}
