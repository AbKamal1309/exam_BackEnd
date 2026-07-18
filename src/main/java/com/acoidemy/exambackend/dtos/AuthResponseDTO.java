package com.acoidemy.exambackend.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponseDTO {
    private UserDTO user;
    private String accessToken;
    private String refreshToken;
}
