package com.example.file_storage.dto;

import com.example.file_storage.entity.ResourceType;

public record ResourceDTO(String path, String name, Long size, ResourceType type) {


}
