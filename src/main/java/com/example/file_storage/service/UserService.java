package com.example.file_storage.service;

import com.example.file_storage.dto.UserCreateUpdateDTO;
import com.example.file_storage.dto.UserDTO;
import com.example.file_storage.entity.UserEntity;
import com.example.file_storage.entity.UserRole;
import com.example.file_storage.exception.UserNameIsTakenException;
import com.example.file_storage.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public UserDTO register(UserCreateUpdateDTO dto) {

        String userName = dto.userName();
        String password = dto.password();

        if (userRepository.findByUserName(userName).isPresent()) {

            throw new UserNameIsTakenException(userName);
        }

        UserEntity userEntity = new UserEntity();
        userEntity.setUserName(userName);
        userEntity.setPassword(encodePassword(password));
        userEntity.setRole(UserRole.USER);

        userRepository.save(userEntity);

        return userEntityToDTO(userEntity);
    }


    private UserDTO userEntityToDTO(UserEntity entity) {

        return new UserDTO(entity.getUserName());
    }

    private String encodePassword(String password) {

        return passwordEncoder.encode(password);

    }


}
