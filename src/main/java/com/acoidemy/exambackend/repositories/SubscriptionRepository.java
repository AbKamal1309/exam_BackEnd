package com.acoidemy.exambackend.repositories;

import com.acoidemy.exambackend.entities.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    Optional<Subscription> findByPurchaseToken(String purchaseToken);

    Optional<Subscription> findFirstByUserIdOrderByIdDesc(Long userId);

    List<Subscription> findByStatusOrderByExpiryDateAsc(String status);
}
