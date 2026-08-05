package com.acoidemy.exambackend.repositories;

import com.acoidemy.exambackend.entities.ManualPremiumRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ManualPremiumRequestRepository extends JpaRepository<ManualPremiumRequest, Long> {

    List<ManualPremiumRequest> findByStatusOrderByCreatedAtAsc(String status);

    Optional<ManualPremiumRequest> findFirstByUserIdAndStatusOrderByCreatedAtDesc(Long userId, String status);
}
