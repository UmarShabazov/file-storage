package com.example.file_storage.dto;

import jakarta.validation.constraints.NotBlank;


public class UserDTO {

    @NotBlank
    private String userName;

    public UserDTO(String userName) {
        this.userName = userName;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

}
