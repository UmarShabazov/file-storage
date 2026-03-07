package com.example.file_storage.repository.storage;

import com.example.file_storage.entity.ResourceType;
import org.springframework.stereotype.Repository;

import java.io.InputStream;
import java.util.Collection;

@Repository
public class MinioStorageRepository implements StorageRepository {

    private final ObjectStorageAdapter storage;
    private final ObjectKeyBuilder keyBuilder;

    public MinioStorageRepository(ObjectStorageAdapter storage, ObjectKeyBuilder keyBuilder) {
        this.storage = storage;
        this.keyBuilder = keyBuilder;
    }

    @Override
    public void putFile(long ownerId, String fullPath, InputStream data, long size) {
        String key = keyBuilder.createKey(ownerId, fullPath);
        storage.put(key, data, size, ResourceType.FILE);
    }

    @Override
    public InputStream downloadFile(long ownerId, String fullPath) {
        String key = keyBuilder.createKey(ownerId, fullPath);
        return storage.download(key);
    }

    @Override
    public void deleteFile(long ownerId, String fullPath) {
        String key = keyBuilder.createKey(ownerId, fullPath);
        storage.delete(key);
    }

    @Override
    public void deleteFiles(long ownerId, Collection<String> fullPaths) {
        if (fullPaths == null || fullPaths.isEmpty()) {
            return;
        }
        for (String path : fullPaths) {
            deleteFile(ownerId, path);
        }
    }

    @Override
    public void copyFile(long ownerId, String fromPath, String toPath) {
        String fromKey = keyBuilder.createKey(ownerId, fromPath);
        String toKey = keyBuilder.createKey(ownerId, toPath);
        storage.copy(fromKey, toKey);
    }
}
