package com.example.file_storage.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class UserCreateUpdateDTO {

    @NotBlank
    @Size(min = 2, message = "Minimal length for username is 2 letters")
    @JsonProperty ("username")
    private String userName;

    @NotBlank
    @Size (min = 5, max = 15, message = "Password should be between 5 and 15 letters")
    private String password;

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public UserCreateUpdateDTO(String userName, String password) {
        this.userName = userName;
        this.password = password;
    }
}
