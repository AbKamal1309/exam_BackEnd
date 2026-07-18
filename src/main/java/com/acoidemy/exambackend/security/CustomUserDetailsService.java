package com.acoidemy.exambackend.security;

import com.acoidemy.exambackend.entities.AppUser;
import com.acoidemy.exambackend.repositories.AppUserRepository;
import lombok.AllArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Adapte AppUser au contrat Spring Security sans toucher à l'entité JPA
 * (on évite ainsi tout risque de conflit avec @EqualsAndHashCode / lazy loading).
 * Le "username" utilisé partout ici est l'EMAIL, car c'est ce que /auth/login reçoit.
 */
@Service
@AllArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final AppUserRepository appUserRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        AppUser appUser = appUserRepository.findByEmail(email);
        if (appUser == null) {
            throw new UsernameNotFoundException("Utilisateur introuvable : " + email);
        }

        List<GrantedAuthority> authorities = appUser.getAppRoles().stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getRoleName().toUpperCase()))
                .collect(Collectors.toList());

        // Le mot de passe stocké ici sert uniquement à satisfaire le contrat UserDetails ;
        // la vérification réelle du mot de passe se fait dans AuthController (bcrypt + fallback legacy).
        return new User(appUser.getEmail(), appUser.getPassword(), authorities);
    }
}
