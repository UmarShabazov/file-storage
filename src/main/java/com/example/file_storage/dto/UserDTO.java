package com.example.file_storage.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;


public record UserDTO(
        @NotBlank
        @JsonProperty("username")
        String userName
) {


}
