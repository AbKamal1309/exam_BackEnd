package com.acoidemy.exambackend.services;

import com.acoidemy.exambackend.dtos.UserDTO;
import com.acoidemy.exambackend.entities.AppRole;
import com.acoidemy.exambackend.exceptions.UserNotFoundException;

import java.util.List;

public interface AppUserService {

UserDTO saveUser(UserDTO userDTO) throws UserNotFoundException;
List<UserDTO> listUsers();
UserDTO getUser(Long userId) throws UserNotFoundException;
UserDTO loadUserByUsername(String username);
UserDTO loadUserByEmail(String email,String password) throws UserNotFoundException;
UserDTO updateUser(UserDTO userDTO) throws UserNotFoundException;

void deleteUser(Long userId);

List<UserDTO> searchUsers(String keyword);

AppRole addNewRole(AppRole appRole);

void addRoleToUser(String username,String roleName);



}




