package com.acoidemy.exambackend.repositories;

import com.acoidemy.exambackend.entities.JoinRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface JoinRequestRepository extends JpaRepository<JoinRequest, Long> {
    List<JoinRequest> findByGroupIdAndStatus(Long groupId, String status);
    boolean existsByGroupIdAndUserIdAndStatus(Long groupId, Long userId, String status);
    List<JoinRequest> findByUserIdAndStatus(Long userId, String status);
}