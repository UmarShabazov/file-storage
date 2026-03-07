package com.example.file_storage.dto;

import com.example.file_storage.entity.ResourceType;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ResourceDTO(String path, String name, Long size, ResourceType type) {


}
