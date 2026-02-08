package com.example.file_storage.entity;

import org.springframework.security.core.authority.SimpleGrantedAuthority;

public enum UserRole {

    USER;

    public SimpleGrantedAuthority toAuthority() {

        return new SimpleGrantedAuthority("ROLE_"+USER);
    }
}
