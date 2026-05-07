package com.acoidemy.exambackend.services;

import com.acoidemy.exambackend.dtos.UserDTO;
import com.acoidemy.exambackend.entities.AppRole;
import com.acoidemy.exambackend.entities.AppUser;
import com.acoidemy.exambackend.exceptions.UserNotFoundException;
import com.acoidemy.exambackend.mappers.ExamMapperImpl;
import com.acoidemy.exambackend.repositories.AppRoleRepository;
import com.acoidemy.exambackend.repositories.AppUserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@Slf4j

public class AppUserServiceImpl implements AppUserService {

    private AppUserRepository appUserRepository;
    private AppRoleRepository appRoleRepository;

    private ExamMapperImpl dtoMapper;



    public AppUserServiceImpl(AppUserRepository appUserRepository, AppRoleRepository appRoleRepository
            , ExamMapperImpl dtoMapper) {
        this.appUserRepository = appUserRepository;
        this.appRoleRepository = appRoleRepository;
        this.dtoMapper = dtoMapper;

    }


    @Override
    public UserDTO saveUser(UserDTO userDTO) throws UserNotFoundException {
        log.info("Saving new AppUser");

       if ( appUserRepository.findByName(userDTO.getName()) !=null)
           throw new UserNotFoundException("User Already Exist");
        AppUser appUser = dtoMapper.fromUserDTO(userDTO);
      // appUser.setPassword(bCryptPasswordEncoder.encode(appUser.getPassword()));
        AppUser savedAppUser = appUserRepository.save(appUser);
        return dtoMapper.fromUser(savedAppUser);
    }

    @Override
    public List<UserDTO> listUsers() {
        List<AppUser> appUsers = appUserRepository.findAll();
        List<UserDTO> userDTOS= appUsers.stream()
                .map(user -> dtoMapper.fromUser(user))
                .collect(Collectors.toList());
        return userDTOS;
    }

    @Override
    public UserDTO getUser(Long userId) throws UserNotFoundException {
        AppUser appUser = appUserRepository.findById(userId)
                .orElseThrow(()->new UserNotFoundException("AppUser Not Found"));
        return dtoMapper.fromUser(appUser);
    }

    @Override
    public UserDTO loadUserByUsername(String username) {
        AppUser appUser = appUserRepository.findByName(username);

        return dtoMapper.fromUser(appUser);
    }

    @Override
    public UserDTO loadUserByEmail(String email,String password) {
        AppUser appUser = appUserRepository.findByEmail(email);

        if (password.equals(appUser.getPassword())){
            return dtoMapper.fromUser(appUser);
        }else {
            return dtoMapper.fromUser(null);
        }


    }

    @Override
    public UserDTO updateUser(UserDTO userDTO) throws UserNotFoundException {
        log.info("Updating AppUser");
        if (appUserRepository.findByName(userDTO.getName()) !=null)
            throw new UserNotFoundException("User Already Exist");
        AppUser appUser =dtoMapper.fromUserDTO(userDTO);
        AppUser savedAppUser = appUserRepository.save(appUser);
        return dtoMapper.fromUser(savedAppUser);
    }

    @Override
    public void deleteUser(Long userId) {
        appUserRepository.deleteById(userId);
    }

    @Override
    public List<UserDTO> searchUsers(String keyword) {
        return null;
    }

    @Override
    public AppRole addNewRole(AppRole appRole) {
        return null;
    }

    @Override
    public void addRoleToUser(String username, String roleName) {
        AppUser appUser = appUserRepository.findByName(username);
        AppRole appRole = appRoleRepository.findByRoleName(roleName);
        appUser.getAppRoles().add(appRole);

    }
}
