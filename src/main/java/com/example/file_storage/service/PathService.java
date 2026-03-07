package com.example.file_storage.service;

import com.example.file_storage.entity.ResourceType;

public class PathService {

    public String normalizePath(String path, ResourceType type) {
        if (type == ResourceType.DIRECTORY && !path.endsWith("/")) {
            return path + "/";
        }
        if (type == ResourceType.FILE && path.endsWith("/")) {
            return path.substring(0, path.length() - 1);
        }
        return path;
    }

    public String extractName(String path) {
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("Path must not be blank");
        }
        if (path.length() == 1) {

            return "/";
        }
        String normalized = path.endsWith("/") ? path.substring(0, path.length() - 1) : path;
        int index = normalized.lastIndexOf("/");
        return normalized.substring(index + 1);
    }

    public String extractParentPath(String path) {
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("Path must not be blank");
        }
        if (path.length() == 1) {

            return "";
        }

        String normalized = path.endsWith("/") ? path.substring(0, path.length() - 1) : path;
        int index = normalized.lastIndexOf("/");
        return normalized.substring(0, index + 1);

    }
}
