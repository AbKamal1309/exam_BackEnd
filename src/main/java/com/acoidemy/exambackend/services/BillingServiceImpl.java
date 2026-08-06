package com.acoidemy.exambackend.services;

import com.acoidemy.exambackend.dtos.ManualPaymentInstructionsDTO;
import com.acoidemy.exambackend.dtos.ManualPremiumRequestCreateDTO;
import com.acoidemy.exambackend.dtos.ManualPremiumRequestDTO;
import com.acoidemy.exambackend.dtos.SubscriptionStatusDTO;
import com.acoidemy.exambackend.dtos.VerifyPurchaseRequestDTO;
import com.acoidemy.exambackend.entities.AppUser;
import com.acoidemy.exambackend.entities.ManualPremiumRequest;
import com.acoidemy.exambackend.entities.Subscription;
import com.acoidemy.exambackend.repositories.AppUserRepository;
import com.acoidemy.exambackend.repositories.ManualPremiumRequestRepository;
import com.acoidemy.exambackend.repositories.SubscriptionRepository;
import com.acoidemy.exambackend.security.SecurityUtils;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.androidpublisher.AndroidPublisher;
import com.google.api.services.androidpublisher.AndroidPublisherScopes;
import com.google.api.services.androidpublisher.model.SubscriptionPurchaseV2;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class BillingServiceImpl implements BillingService {

    // Contenu JSON complet de la clé du compte de service Google Cloud (Play
    // Console → Setup → API access), collé tel quel en variable d'environnement
    // sur Render — jamais commité dans le dépôt.
    @Value("${GOOGLE_PLAY_SERVICE_ACCOUNT_JSON:}")
    private String serviceAccountJson;

    @Value("${app.android.package-name:com.examapp}")
    private String packageName;

    // Quotas du palier gratuit — voir aussi SubscriptionStatusDTO.
    private static final int FREE_AI_QUOTA = 10;
    private static final int FREE_GROUP_QUOTA = 1;

    // ── Activation manuelle (RIB / WhatsApp) — solution de repli pendant que
    // Google Play Billing (bloqué au Maroc) et une passerelle en ligne sont en
    // cours de mise en place. Configurable par variables d'environnement pour ne
    // jamais coder ces infos "en dur" et pouvoir les changer sans redéploiement.
    @Value("${app.manual-payment.bank-rib:}")
    private String manualBankRib;
    @Value("${app.manual-payment.bank-name:}")
    private String manualBankName;
    @Value("${app.manual-payment.whatsapp:}")
    private String manualWhatsapp;
    @Value("${app.manual-payment.monthly-price-mad:29}")
    private double manualMonthlyPriceMad;
    @Value("${app.manual-payment.annual-price-mad:290}")
    private double manualAnnualPriceMad;

    private final AppUserRepository appUserRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final ManualPremiumRequestRepository manualPremiumRequestRepository;
    private final SecurityUtils securityUtils;

    private AndroidPublisher androidPublisher;

    @PostConstruct
    public void init() {
        if (serviceAccountJson == null || serviceAccountJson.isBlank()) {
            log.warn("GOOGLE_PLAY_SERVICE_ACCOUNT_JSON non configuré : la vérification " +
                    "des achats Google Play est désactivée (verifyPurchase échouera).");
            return;
        }
        try {
            GoogleCredential credential = GoogleCredential
                    .fromStream(new ByteArrayInputStream(serviceAccountJson.getBytes(StandardCharsets.UTF_8)))
                    .createScoped(Collections.singleton(AndroidPublisherScopes.ANDROIDPUBLISHER));
            androidPublisher = new AndroidPublisher.Builder(
                    GoogleNetHttpTransport.newTrustedTransport(),
                    GsonFactory.getDefaultInstance(),
                    credential
            ).setApplicationName("ExamPlatform").build();
        } catch (Exception e) {
            log.error("Échec d'initialisation du client Google Play Developer API", e);
        }
    }

    @Override
    @Transactional
    public SubscriptionStatusDTO verifyPurchase(VerifyPurchaseRequestDTO request, Authentication authentication) throws Exception {
        if (androidPublisher == null) {
            throw new IllegalStateException("La vérification des achats n'est pas configurée côté serveur pour le moment.");
        }
        AppUser user = securityUtils.getCurrentUser(authentication);

        // Idempotence : un même purchaseToken ne doit jamais être traité deux fois
        // (retry réseau côté mobile, double-appel, etc.).
        if (subscriptionRepository.findByPurchaseToken(request.getPurchaseToken()).isPresent()) {
            return buildStatus(user);
        }

        SubscriptionPurchaseV2 purchase = androidPublisher.purchases()
                .subscriptionsv2()
                .get(packageName, request.getPurchaseToken())
                .execute();

        String state = purchase.getSubscriptionState();
        boolean active = "SUBSCRIPTION_STATE_ACTIVE".equals(state)
                || "SUBSCRIPTION_STATE_IN_GRACE_PERIOD".equals(state);

        LocalDateTime expiry = null;
        if (purchase.getLineItems() != null && !purchase.getLineItems().isEmpty()) {
            String expiryTime = purchase.getLineItems().get(0).getExpiryTime();
            if (expiryTime != null) {
                expiry = OffsetDateTime.parse(expiryTime).atZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
            }
        }

        Subscription sub = new Subscription();
        sub.setUser(user);
        sub.setProductId(request.getProductId());
        sub.setPurchaseToken(request.getPurchaseToken());
        sub.setOrderId(purchase.getLatestOrderId());
        sub.setExpiryDate(expiry);
        sub.setStatus(active ? "ACTIVE" : "EXPIRED");
        subscriptionRepository.save(sub);

        if (active && expiry != null) {
            user.setPremiumUntil(expiry);
            appUserRepository.save(user);
        }

        return buildStatus(user);
    }

    @Override
    public SubscriptionStatusDTO getStatus(Authentication authentication) {
        AppUser user = securityUtils.getCurrentUser(authentication);
        return buildStatus(user);
    }

    @Override
    @Transactional
    public void checkAiQuota(AppUser user) {
        if (user.isPremium() || securityUtils.isAdminUser(user)) return;
        resetQuotaIfNeeded(user);
        if (user.getAiQuestionsUsedThisMonth() >= FREE_AI_QUOTA) {
            throw new RuntimeException(
                    "Quota de génération IA atteint (" + FREE_AI_QUOTA + " questions/mois en version gratuite). " +
                            "Passez à l'abonnement premium pour une génération illimitée.");
        }
    }

    @Override
    @Transactional
    public void incrementAiUsage(AppUser user) {
        if (user.isPremium()) return; // pas de compteur à tenir pour un compte premium
        user.setAiQuestionsUsedThisMonth(user.getAiQuestionsUsedThisMonth() + 1);
        appUserRepository.save(user);
    }

    @Override
    public void checkGroupQuota(AppUser user) {
        if (user.isPremium() || securityUtils.isAdminUser(user)) return;
        int created = user.getCreatedGroups() != null ? user.getCreatedGroups().size() : 0;
        if (created >= FREE_GROUP_QUOTA) {
            throw new RuntimeException(
                    "Limite de " + FREE_GROUP_QUOTA + " groupe(s) créé(s) atteinte en version gratuite. " +
                            "Passez à l'abonnement premium pour créer des groupes illimités.");
        }
    }

    private SubscriptionStatusDTO buildStatus(AppUser user) {
        resetQuotaIfNeeded(user);

        String currentPlan = null;
        String provider = null;
        if (user.isPremium()) {
            Subscription latest = subscriptionRepository.findFirstByUserIdOrderByIdDesc(user.getId()).orElse(null);
            if (latest != null && latest.getProductId() != null) {
                currentPlan = latest.getProductId().toLowerCase().contains("annual") ? "ANNUAL" : "MONTHLY";
                provider = latest.getProductId().startsWith("manual_") ? "MANUAL" : "GOOGLE_PLAY";
            }
        }

        return SubscriptionStatusDTO.builder()
                .premium(user.isPremium())
                .premiumUntil(user.getPremiumUntil())
                .currentPlan(currentPlan)
                .provider(provider)
                .aiQuestionsUsedThisMonth(user.getAiQuestionsUsedThisMonth())
                .aiQuestionsQuota(FREE_AI_QUOTA)
                .groupsCreated(user.getCreatedGroups() != null ? user.getCreatedGroups().size() : 0)
                .groupsQuota(FREE_GROUP_QUOTA)
                .build();
    }

    private void resetQuotaIfNeeded(AppUser user) {
        LocalDateTime now = LocalDateTime.now();
        if (user.getAiQuotaResetAt() == null || now.isAfter(user.getAiQuotaResetAt())) {
            user.setAiQuestionsUsedThisMonth(0);
            user.setAiQuotaResetAt(now.plusMonths(1));
            appUserRepository.save(user);
        }
    }

    // ── Activation premium manuelle ──────────────────────────────────────────

    @Override
    public ManualPaymentInstructionsDTO getManualPaymentInstructions() {
        return ManualPaymentInstructionsDTO.builder()
                .bankRib(manualBankRib)
                .bankName(manualBankName)
                .whatsappNumber(manualWhatsapp)
                .monthlyPriceMad(manualMonthlyPriceMad)
                .annualPriceMad(manualAnnualPriceMad)
                .build();
    }

    @Override
    @Transactional
    public ManualPremiumRequestDTO createManualRequest(ManualPremiumRequestCreateDTO dto, Authentication authentication) {
        AppUser user = securityUtils.getCurrentUser(authentication);

        if (!"MONTHLY".equals(dto.getPlan()) && !"ANNUAL".equals(dto.getPlan())) {
            throw new RuntimeException("Plan invalide : doit être MONTHLY ou ANNUAL.");
        }

        // Évite d'empiler plusieurs demandes en double pour le même utilisateur.
        if (manualPremiumRequestRepository.findFirstByUserIdAndStatusOrderByCreatedAtDesc(user.getId(), "PENDING").isPresent()) {
            throw new RuntimeException("Vous avez déjà une demande en attente de validation.");
        }

        ManualPremiumRequest req = new ManualPremiumRequest();
        req.setUser(user);
        req.setPlan(dto.getPlan());
        req.setPaymentReference(dto.getPaymentReference());
        req.setStatus("PENDING");
        manualPremiumRequestRepository.save(req);

        return toManualDto(req);
    }

    @Override
    public ManualPremiumRequestDTO getMyPendingManualRequest(Authentication authentication) {
        AppUser user = securityUtils.getCurrentUser(authentication);
        return manualPremiumRequestRepository
                .findFirstByUserIdAndStatusOrderByCreatedAtDesc(user.getId(), "PENDING")
                .map(this::toManualDto)
                .orElse(null);
    }

    @Override
    public List<ManualPremiumRequestDTO> getPendingManualRequests(Authentication authentication) {
        requireAdmin(authentication);
        return manualPremiumRequestRepository.findByStatusOrderByCreatedAtAsc("PENDING")
                .stream().map(this::toManualDto).toList();
    }

    @Override
    @Transactional
    public ManualPremiumRequestDTO approveManualRequest(Long requestId, Authentication authentication) {
        AppUser admin = requireAdmin(authentication);
        ManualPremiumRequest req = manualPremiumRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Demande introuvable."));
        if (!"PENDING".equals(req.getStatus())) {
            throw new RuntimeException("Cette demande a déjà été traitée.");
        }

        AppUser user = req.getUser();
        LocalDateTime base = user.isPremium() ? user.getPremiumUntil() : LocalDateTime.now();
        LocalDateTime newExpiry = "ANNUAL".equals(req.getPlan()) ? base.plusYears(1) : base.plusMonths(1);
        user.setPremiumUntil(newExpiry);
        appUserRepository.save(user);

        req.setStatus("APPROVED");
        req.setProcessedAt(LocalDateTime.now());
        req.setProcessedByUserId(admin.getId());
        manualPremiumRequestRepository.save(req);

        // Trace dans Subscription pour garder un historique unifié, cohérent avec
        // les achats Google Play — provider distingue l'origine.
        Subscription sub = new Subscription();
        sub.setUser(user);
        sub.setProductId("MONTHLY".equals(req.getPlan()) ? "manual_monthly" : "manual_annual");
        sub.setPurchaseToken("manual-" + req.getId()); // unique, jamais réutilisé par Google
        sub.setExpiryDate(newExpiry);
        sub.setStatus("ACTIVE");
        subscriptionRepository.save(sub);

        return toManualDto(req);
    }

    @Override
    @Transactional
    public ManualPremiumRequestDTO rejectManualRequest(Long requestId, Authentication authentication) {
        AppUser admin = requireAdmin(authentication);
        ManualPremiumRequest req = manualPremiumRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Demande introuvable."));
        if (!"PENDING".equals(req.getStatus())) {
            throw new RuntimeException("Cette demande a déjà été traitée.");
        }
        req.setStatus("REJECTED");
        req.setProcessedAt(LocalDateTime.now());
        req.setProcessedByUserId(admin.getId());
        manualPremiumRequestRepository.save(req);
        return toManualDto(req);
    }

    private AppUser requireAdmin(Authentication authentication) {
        AppUser user = securityUtils.getCurrentUser(authentication);
        if (!securityUtils.isAdminUser(user)) {
            throw new RuntimeException("Réservé aux administrateurs.");
        }
        return user;
    }

    private ManualPremiumRequestDTO toManualDto(ManualPremiumRequest req) {
        AppUser user = req.getUser();
        return ManualPremiumRequestDTO.builder()
                .id(req.getId())
                .userId(user != null ? user.getId() : null)
                .userName(user != null ? user.getName() : null)
                .userEmail(user != null ? user.getEmail() : null)
                .plan(req.getPlan())
                .paymentReference(req.getPaymentReference())
                .status(req.getStatus())
                .createdAt(req.getCreatedAt())
                .build();
    }
}
