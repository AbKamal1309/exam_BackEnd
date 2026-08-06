package com.acoidemy.exambackend.web;

import com.acoidemy.exambackend.dtos.ManualPaymentInstructionsDTO;
import com.acoidemy.exambackend.dtos.ManualPremiumRequestCreateDTO;
import com.acoidemy.exambackend.dtos.ManualPremiumRequestDTO;
import com.acoidemy.exambackend.dtos.SubscriptionStatusDTO;
import com.acoidemy.exambackend.dtos.VerifyPurchaseRequestDTO;
import com.acoidemy.exambackend.services.BillingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    // ── Activation premium manuelle (RIB / WhatsApp) ──────────────────────────

    @GetMapping("/manual/instructions")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ManualPaymentInstructionsDTO> getManualInstructions() {
        return ResponseEntity.ok(billingService.getManualPaymentInstructions());
    }

    @PostMapping("/manual/request")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ManualPremiumRequestDTO> createManualRequest(
            @RequestBody ManualPremiumRequestCreateDTO request,
            Authentication authentication) {
        return ResponseEntity.ok(billingService.createManualRequest(request, authentication));
    }

    @GetMapping("/manual/my-request")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ManualPremiumRequestDTO> getMyManualRequest(Authentication authentication) {
        ManualPremiumRequestDTO dto = billingService.getMyPendingManualRequest(authentication);
        // 204 explicite plutôt qu'un corps 200 vide et ambigu : Retrofit/Gson côté
        // mobile plantait en essayant de parser un corps de 0 octet ("End of input").
        return dto != null ? ResponseEntity.ok(dto) : ResponseEntity.noContent().build();
    }

    // Réservé admin — la vérification isAdminUser() se fait aussi dans le service
    // (défense en profondeur), mais @PreAuthorize coupe court dès la requête HTTP.
    @GetMapping("/manual/requests")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ManualPremiumRequestDTO>> getPendingManualRequests(Authentication authentication) {
        return ResponseEntity.ok(billingService.getPendingManualRequests(authentication));
    }

    @PostMapping("/manual/requests/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ManualPremiumRequestDTO> approveManualRequest(
            @PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(billingService.approveManualRequest(id, authentication));
    }

    @PostMapping("/manual/requests/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ManualPremiumRequestDTO> rejectManualRequest(
            @PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(billingService.rejectManualRequest(id, authentication));
    }
}
