package com.example.file_storage.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;


@Entity
@Table(name = "users")
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @NotBlank
    @Column(name = "name", length = 20, unique = true)
    @Size(min = 1, message = "User name should be greater than 1 letter")
    private String userName;

    @NotBlank
    @Column(name = "password")
    private String password;


    @Column(name = "role")
    @Enumerated (EnumType.STRING)
    private UserRole role;

    public UserEntity() {
    }

    public UserEntity(UserRole role, String password, String userName) {
        this.role = role;
        this.password = password;
        this.userName = userName;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

}
