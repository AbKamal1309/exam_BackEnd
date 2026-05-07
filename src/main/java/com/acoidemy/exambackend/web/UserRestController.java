package com.acoidemy.exambackend.web;

import com.acoidemy.exambackend.dtos.UserDTO;
import com.acoidemy.exambackend.exceptions.UserNotFoundException;
import com.acoidemy.exambackend.services.AppUserService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@AllArgsConstructor
@Slf4j
@CrossOrigin(origins = "http://localhost:4200")
public class UserRestController {

    private AppUserService userService;


    @GetMapping("/users")
    public List<UserDTO> users(){
        return userService.listUsers();
    }
    @GetMapping("/users/{id}")
    public UserDTO getUser(@PathVariable(name = "id") Long userId) throws UserNotFoundException{
        return userService.getUser(userId);
    }
    @PostMapping("/users")
    public UserDTO saveCustomer(@RequestBody UserDTO userDTO) throws UserNotFoundException {
        return userService.saveUser(userDTO);
    }

    @PostMapping("/login")
    public UserDTO login(@RequestBody UserDTO userDTO) throws UserNotFoundException{
        return userService.loadUserByEmail(userDTO.getEmail(),userDTO.getPassword());
    }
    @PutMapping("/users/{userId}")
    public UserDTO updateUser(@PathVariable Long userId, @RequestBody UserDTO userDTO) throws UserNotFoundException {

        return userService.updateUser(userDTO);
    }

    @DeleteMapping("/users/{id}")
    public void deleteUser(@PathVariable Long id){
       userService.deleteUser(id);
    }
}
