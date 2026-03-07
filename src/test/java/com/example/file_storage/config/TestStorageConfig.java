package com.example.file_storage.config;

import com.example.file_storage.entity.ResourceType;
import com.example.file_storage.exception.StorageException;
import com.example.file_storage.repository.storage.ObjectStorageAdapter;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@TestConfiguration
@Profile("test")
public class TestStorageConfig {

    @Bean
    @Primary
    public ObjectStorageAdapter objectStorageAdapter() {
        return new InMemoryObjectStorageAdapter();
    }

    static class InMemoryObjectStorageAdapter implements ObjectStorageAdapter {

        private final Map<String, byte[]> store = new ConcurrentHashMap<>();

        @Override
        public void ensureBucketExists() {
            // No-op for tests.
        }

        @Override
        public Optional<ObjectStat> stat(String objectKey) {
            byte[] data = store.get(objectKey);
            if (data == null) {
                return Optional.empty();
            }
            return Optional.of(new ObjectStat(objectKey, data.length, "", ""));
        }

        @Override
        public void put(String objectKey, InputStream data, long size, ResourceType contentType) {
            try {
                store.put(objectKey, data.readAllBytes());
            } catch (IOException e) {
                throw new StorageException("Failed to read input stream.");
            }
        }

        @Override
        public void delete(String objectKey) {
            store.remove(objectKey);
        }

        @Override
        public void deleteBatch(Collection<String> objectKeys) {
            for (String key : objectKeys) {
                store.remove(key);
            }
        }

        @Override
        public void copy(String fromKey, String toKey) {
            byte[] data = store.get(fromKey);
            if (data == null) {
                throw new StorageException("Object not found: " + fromKey);
            }
            store.put(toKey, data);
        }

        @Override
        public InputStream download(String key) {
            byte[] data = store.get(key);
            if (data == null) {
                throw new StorageException("Object not found: " + key);
            }
            return new ByteArrayInputStream(data);
        }
    }
}
