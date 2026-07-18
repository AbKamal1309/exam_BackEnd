package com.acoidemy.exambackend.web;

import com.acoidemy.exambackend.dtos.UserDTO;
import com.acoidemy.exambackend.exceptions.UserNotFoundException;
import com.acoidemy.exambackend.security.SecurityUtils;
import com.acoidemy.exambackend.services.AppUserService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@AllArgsConstructor
@Slf4j
public class UserRestController {

    private AppUserService userService;
    private SecurityUtils securityUtils;

    @GetMapping("/users")
    public List<UserDTO> users() {
        return userService.listUsers();
    }

    @GetMapping("/users/{id}")
    public UserDTO getUser(@PathVariable(name = "id") Long userId) throws UserNotFoundException {
        return userService.getUser(userId);
    }

    // Inscription. Le mot de passe est haché en BCrypt dans AppUserServiceImpl.saveUser().
    @PostMapping("/users")
    public UserDTO saveCustomer(@RequestBody UserDTO userDTO) throws UserNotFoundException {
        return userService.saveUser(userDTO);
    }

    // NOTE: le login se fait désormais via POST /auth/login (voir AuthController).

    @PutMapping("/users/{userId}")
    @PreAuthorize("isAuthenticated()")
    public UserDTO updateUser(@PathVariable Long userId, @RequestBody UserDTO userDTO, Authentication authentication)
            throws UserNotFoundException {
        Long requesterId = securityUtils.getCurrentUserId(authentication);
        if (!requesterId.equals(userId) && !securityUtils.isAdmin(authentication)) {
            throw new RuntimeException("Vous ne pouvez modifier que votre propre compte.");
        }
        return userService.updateUser(userDTO);
    }

    @DeleteMapping("/users/{id}")
    @PreAuthorize("isAuthenticated()")
    public void deleteUser(@PathVariable Long id, Authentication authentication) {
        Long requesterId = securityUtils.getCurrentUserId(authentication);
        if (!requesterId.equals(id) && !securityUtils.isAdmin(authentication)) {
            throw new RuntimeException("Vous ne pouvez supprimer que votre propre compte.");
        }
        userService.deleteUser(id);
    }
}
