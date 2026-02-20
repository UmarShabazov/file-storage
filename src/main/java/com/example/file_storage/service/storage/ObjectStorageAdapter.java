package com.example.file_storage.service.storage;

import com.example.file_storage.entity.ResourceType;
import com.example.file_storage.exception.StorageException;

import java.io.InputStream;
import java.time.Instant;
import java.util.Collection;
import java.util.Optional;

public interface ObjectStorageAdapter {

    void ensureBucketExists() throws StorageException;

    Optional<ObjectStat> stat(String objectKey) throws StorageException;

    void put(String objectKey, InputStream data, long size, ResourceType contentType) throws StorageException;

    InputStream get(String objectKey) throws StorageException;

    void delete(String objectKey) throws StorageException;

    void copy(String fromKey, String toKey) throws StorageException;


    void deleteBatch(Collection<String> objectKeys) throws StorageException;

    record ObjectStat(String key, long size, String etag, String contentType) {
    }


}
