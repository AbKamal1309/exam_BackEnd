package com.acoidemy.exambackend.dtos;

import lombok.Data;
import lombok.Builder;
import java.time.LocalDateTime;

@Data
@Builder
public class JoinRequestDTO {
    private Long id;
    private Long groupId;
    private String groupName;
    private Long userId;
    private String userName;
    private String userEmail;
    private String status;
    private LocalDateTime requestDate;
}