package com.acoidemy.exambackend.web;

import com.acoidemy.exambackend.dtos.AuthResponseDTO;
import com.acoidemy.exambackend.dtos.UserDTO;
import com.acoidemy.exambackend.entities.AppUser;
import com.acoidemy.exambackend.exceptions.UserNotFoundException;
import com.acoidemy.exambackend.mappers.ExamMapperImpl;
import com.acoidemy.exambackend.repositories.AppUserRepository;
import com.acoidemy.exambackend.security.CustomUserDetailsService;
import com.acoidemy.exambackend.security.JwtService;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.UUID;

@RestController
@RequestMapping("/auth")
@Slf4j
public class AuthController {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;
    private final ExamMapperImpl dtoMapper;

    @Value("${app.google.client-id:744131153960-dg5pk850gjgdbj5dbu7255ql1kv7lvqr.apps.googleusercontent.com}")
    private String googleClientId;

    public AuthController(AppUserRepository appUserRepository,
                          PasswordEncoder passwordEncoder,
                          JwtService jwtService,
                          CustomUserDetailsService userDetailsService,
                          ExamMapperImpl dtoMapper) {
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.dtoMapper = dtoMapper;
    }

    @PostMapping("/login")
    public AuthResponseDTO login(@RequestBody UserDTO request) throws UserNotFoundException {
        AppUser appUser = appUserRepository.findByEmail(request.getEmail());
        if (appUser == null) {
            throw new UserNotFoundException("Email ou mot de passe incorrect");
        }

        String stored = appUser.getPassword();
        boolean matches;

        if (isBcryptHash(stored)) {
            matches = passwordEncoder.matches(request.getPassword(), stored);
        } else {
            matches = stored != null && stored.equals(request.getPassword());
            if (matches) {
                log.info("Migration du mot de passe en clair vers bcrypt pour l'utilisateur {}", appUser.getEmail());
                appUser.setPassword(passwordEncoder.encode(request.getPassword()));
                appUserRepository.save(appUser);
            }
        }

        if (!matches) {
            throw new UserNotFoundException("Email ou mot de passe incorrect");
        }

        return buildAuthResponse(appUser);
    }

    /**
     * Connexion via Google Identity Services.
     * Le frontend envoie le "credential" (id_token) brut renvoyé par Google — on le
     * VÉRIFIE ici côté serveur (signature + audience) avant de faire confiance à l'email.
     * Ne jamais faire confiance à un email/nom envoyé directement par le client pour ce flow.
     */
    @PostMapping("/google")
    public AuthResponseDTO googleLogin(@RequestBody GoogleLoginRequest request) throws UserNotFoundException {
        if (googleClientId == null || googleClientId.isBlank()) {
            throw new IllegalStateException("app.google.client-id n'est pas configuré côté serveur");
        }

        GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new GsonFactory())
                .setAudience(Collections.singletonList(googleClientId))
                .build();

        GoogleIdToken idToken;
        try {
            idToken = verifier.verify(request.getIdToken());
        } catch (Exception e) {
            throw new UserNotFoundException("Token Google invalide");
        }

        if (idToken == null) {
            throw new UserNotFoundException("Token Google invalide ou expiré");
        }

        GoogleIdToken.Payload payload = idToken.getPayload();
        String email = payload.getEmail();
        String name = (String) payload.get("name");

        AppUser appUser = appUserRepository.findByEmail(email);
        if (appUser == null) {
            log.info("Création d'un compte via Google pour {}", email);
            appUser = new AppUser();
            appUser.setEmail(email);
            appUser.setName(name != null && !name.isBlank() ? name : email);
            // Mot de passe aléatoire, jamais communiqué ni utilisable pour un login classique tant
            // qu'il n'est pas explicitement redéfini par l'utilisateur.
            appUser.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
            appUser = appUserRepository.save(appUser);
        }

        return buildAuthResponse(appUser);
    }

    @PostMapping("/refresh")
    public AuthResponseDTO refresh(@RequestBody RefreshRequest request) throws UserNotFoundException {
        String email;
        try {
            email = jwtService.extractEmail(request.getRefreshToken());
        } catch (Exception e) {
            throw new UserNotFoundException("Refresh token invalide ou expiré");
        }

        AppUser appUser = appUserRepository.findByEmail(email);
        if (appUser == null) {
            throw new UserNotFoundException("Utilisateur introuvable");
        }

        UserDetails userDetails = userDetailsService.loadUserByUsername(email);
        if (!jwtService.isTokenValid(request.getRefreshToken(), userDetails)) {
            throw new UserNotFoundException("Refresh token invalide ou expiré");
        }

        String newAccessToken = jwtService.generateAccessToken(userDetails);
        return new AuthResponseDTO(dtoMapper.fromUser(appUser), newAccessToken, request.getRefreshToken());
    }

    private AuthResponseDTO buildAuthResponse(AppUser appUser) {
        UserDetails userDetails = userDetailsService.loadUserByUsername(appUser.getEmail());
        String accessToken = jwtService.generateAccessToken(userDetails);
        String refreshToken = jwtService.generateRefreshToken(userDetails);
        return new AuthResponseDTO(dtoMapper.fromUser(appUser), accessToken, refreshToken);
    }

    private boolean isBcryptHash(String value) {
        return value != null && (value.startsWith("$2a$") || value.startsWith("$2b$") || value.startsWith("$2y$"));
    }

    @Getter
    @Setter
    public static class RefreshRequest {
        private String refreshToken;
    }

    @Getter
    @Setter
    public static class GoogleLoginRequest {
        private String idToken;
    }
}