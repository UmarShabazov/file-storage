package com.example.file_storage.repository;

import com.example.file_storage.entity.ResourceEntity;
import com.example.file_storage.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface ResourceRepository extends JpaRepository <ResourceEntity, Long> {

    Optional<ResourceEntity> findByOwnerAndFullPath(UserEntity owner, String path);

    void deleteByOwnerAndFullPath(UserEntity owner, String path);

    List<ResourceEntity> findAllByOwnerAndFullPathStartingWith(UserEntity owner, String path);

    Page<ResourceEntity> findAllByOwnerAndNameContainingIgnoreCase(UserEntity owner, String query, Pageable pageable);
    List<ResourceEntity> findAllByOwnerAndParentPath(UserEntity owner, String parentPath);

    boolean existsByOwnerAndFullPath(UserEntity owner, String path);
}
