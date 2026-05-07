package com.acoidemy.exambackend.repositories;

import com.acoidemy.exambackend.entities.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser,Long> {
    AppUser findByName(String username);
    AppUser findByEmail(String email);
    Optional<AppUser> findById(Long id);
}
