package com.example.file_storage.repository;

import com.example.file_storage.entity.ResourceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResourceRepository extends JpaRepository <ResourceEntity, Long> {

}
