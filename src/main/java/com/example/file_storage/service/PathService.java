package com.example.file_storage.service;

import com.example.file_storage.entity.ResourceType;
import org.springframework.stereotype.Service;

@Service
public class PathService {

    public String normalizePath(String path, ResourceType type) {
        if (path == null) {
            throw new IllegalArgumentException("Path must not be null");
        }

        String normalized = normalizeSeparators(path);

        if (normalized.isBlank()) {
            if (type == ResourceType.DIRECTORY && normalized.isEmpty()) {
                return "";
            }
            throw new IllegalArgumentException("Path must not be blank");
        }

        validateRelativePath(normalized);

        if (type == ResourceType.DIRECTORY) {
            return normalized.endsWith("/") ? normalized : normalized + "/";
        }

        if (normalized.endsWith("/")) {
            throw new IllegalArgumentException("File path must not end with '/'");
        }

        return normalized;
    }

    public String normalizeUploadRelativePath(String path) {
        String normalized = normalizePath(path, ResourceType.FILE);
        if (normalized.indexOf('/') < 0) {
            return normalized;
        }

        return normalized;
    }

    public String extractName(String path) {
        if (path == null) {
            throw new IllegalArgumentException("Path must not be null");
        }
        if (path.isEmpty()) {
            return "";
        }

        String normalized = normalizeForExtraction(path);
        int index = normalized.lastIndexOf("/");
        return normalized.substring(index + 1);
    }

    public String extractParentPath(String path) {
        if (path == null) {
            throw new IllegalArgumentException("Path must not be null");
        }
        if (path.isEmpty()) {
            return "";
        }

        String normalized = normalizeForExtraction(path);
        int index = normalized.lastIndexOf("/");
        if (index < 0) {
            return "";
        }
        return normalized.substring(0, index + 1);
    }

    private String normalizeForExtraction(String path) {
        String normalized = normalizeSeparators(path);
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("Path must not be blank");
        }
        return normalized.endsWith("/") ? normalized.substring(0, normalized.length() - 1) : normalized;
    }

    private String normalizeSeparators(String path) {
        return path.replace('\\', '/');
    }

    private void validateRelativePath(String path) {
        if (path.startsWith("/")) {
            throw new IllegalArgumentException("Path must be relative");
        }
        if (path.contains("//")) {
            throw new IllegalArgumentException("Path must not contain empty segments");
        }

        String candidate = path.endsWith("/") ? path.substring(0, path.length() - 1) : path;
        for (String segment : candidate.split("/")) {
            if (segment.isBlank()) {
                throw new IllegalArgumentException("Path must not contain blank names");
            }
            if (".".equals(segment) || "..".equals(segment)) {
                throw new IllegalArgumentException("Path must not contain '.' or '..' segments");
            }
        }

    }
}
