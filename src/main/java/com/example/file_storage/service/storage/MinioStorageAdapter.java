package com.example.file_storage.service.storage;

import com.example.file_storage.entity.ResourceType;
import com.example.file_storage.exception.StorageException;
import io.minio.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.io.InputStream;
import java.util.Collection;
import java.util.Optional;

public class MinioStorageAdapter implements ObjectStorageAdapter {

    private final MinioClient minioClient;

    private final String bucket;

    @Autowired
    public MinioStorageAdapter(MinioClient minioClient, @Value("${minio.bucket}") String bucket) {
        this.minioClient = minioClient;
        this.bucket = bucket;
    }

    @Override
    public void ensureBucketExists() throws StorageException {
        try {
            boolean found = minioClient.bucketExists(
                    BucketExistsArgs
                            .builder()
                            .bucket(bucket)
                            .build());

            if (!found) {
                minioClient.makeBucket(MakeBucketArgs
                        .builder()
                        .bucket(bucket)
                        .build());
                System.out.println("Создали бакет " + bucket + ".");
            } else {
                System.out.println(bucket + " уже был создан.");
            }
        } catch (Exception e) {
            throw new StorageException(e.getMessage());
        }

    }

    @Override
    public Optional<ObjectStat> stat(String objectKey) throws StorageException {

        try {
            StatObjectResponse response = minioClient.statObject(
                    StatObjectArgs.builder().bucket(bucket).object(objectKey).build());

            return Optional.of(new ObjectStat(
                    objectKey,
                    response.size(),
                    response.etag(),
                    response.contentType()
            ));
        } catch (Exception e) {

            throw new StorageException(e.getMessage());

        }
    }


    @Override
    public void put(String objectKey, InputStream data, long size, ResourceType contentType) throws StorageException {

    }

    @Override
    public InputStream get(String objectKey) throws StorageException {
        return null;
    }

    @Override
    public void delete(String objectKey) throws StorageException {

    }

    @Override
    public void copy(String fromKey, String toKey) throws StorageException {

    }

    @Override
    public void deleteBatch(Collection<String> objectKeys) throws StorageException {

    }

}
