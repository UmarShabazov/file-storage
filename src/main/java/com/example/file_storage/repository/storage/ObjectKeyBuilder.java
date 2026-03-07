package com.example.file_storage.repository.storage;

import org.springframework.stereotype.Component;

@Component
public class ObjectKeyBuilder {

    public String createKey(Long userId, String path){

        return "user-"+userId+"-"+path;
    }
}
