package com.example.file_storage.repository;

import com.example.file_storage.dto.UserDTO;
import com.example.file_storage.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long> {

    Optional <UserEntity> findByUserName(String userName);
}
