package com.acoidemy.exambackend.dtos;

import lombok.Data;

import java.util.List;

@Data
public class UserDTO {

    private Long id;
    private String name;
    private String email;

    private String password;

    private List<String> roles;

}
