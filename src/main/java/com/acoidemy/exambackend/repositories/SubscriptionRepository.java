package com.acoidemy.exambackend.repositories;

import com.acoidemy.exambackend.entities.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    Optional<Subscription> findByPurchaseToken(String purchaseToken);
}
