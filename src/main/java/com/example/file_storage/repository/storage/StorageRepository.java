package com.example.file_storage.repository.storage;

import java.io.InputStream;
import java.util.Collection;

public interface StorageRepository {

    void putFile(long ownerId, String fullPath, InputStream data, long size);

    InputStream downloadFile(long ownerId, String fullPath);

    void deleteFile(long ownerId, String fullPath);

    void deleteFiles(long ownerId, Collection<String> fullPaths);

    void copyFile(long ownerId, String fromPath, String toPath);
}
