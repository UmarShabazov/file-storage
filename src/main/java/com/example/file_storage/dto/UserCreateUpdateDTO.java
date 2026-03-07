package com.example.file_storage.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserCreateUpdateDTO(

        @NotBlank
        @Size(min = 2, message = "Minimal length for username is 2 letters")
        @JsonProperty("username")
        String userName,

        @NotBlank
        @Size(min = 5, max = 15, message = "Password should be between 5 and 15 letters")
        String password)
{}
