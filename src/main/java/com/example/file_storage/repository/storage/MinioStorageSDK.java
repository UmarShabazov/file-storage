package com.example.file_storage.repository.storage;

import com.example.file_storage.entity.ResourceType;
import com.example.file_storage.exception.StorageException;
import io.minio.*;
import io.minio.errors.ErrorResponseException;
import io.minio.messages.DeleteError;
import io.minio.messages.DeleteObject;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Component
@Profile("!test")
public class MinioStorageSDK implements ObjectStorageAdapter {

    private final MinioClient minioClient;

    private final String bucket;

    @Autowired
    public MinioStorageSDK(MinioClient minioClient, @Value("${minio.bucket}") String bucket) {
        this.minioClient = minioClient;
        this.bucket = bucket;
    }

    @PostConstruct
    public void init() {
        ensureBucketExists();
    }


    @Override
    public void ensureBucketExists() {
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
                System.out.println("Created bucket" + bucket + ".");
            } else {
                System.out.println(bucket + " was already created.");
            }
        } catch (Exception e) {
            throw new StorageException(e.getMessage());
        }

    }

    @Override
    public Optional<ObjectStat> stat(String objectKey) {
        try {
            StatObjectResponse response = minioClient.statObject(
                    StatObjectArgs.builder().bucket(bucket).object(objectKey).build()
            );

            return Optional.of(new ObjectStat(
                    objectKey,
                    response.size(),
                    response.etag(),
                    response.contentType()
            ));
        } catch (ErrorResponseException e) {
            String code = e.errorResponse().code();
            if ("NoSuchKey".equals(code) || "NotFound".equals(code)) {
                return Optional.empty();
            }
            throw new StorageException("Minio error: " + code);
        } catch (Exception e) {
            throw new StorageException("Minio stat failed: " + e.getMessage());
        }
    }


    @Override
    public void put(String objectKey, InputStream data, long size, ResourceType contentType) {

        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectKey)
                            .stream(data, size, -1)
                            .build()
            );
        }
        catch (Exception e) {

            throw new StorageException(e.getMessage());
        }

    }

    @Override
    public InputStream download(String objectKey) {

        try {
            return minioClient.getObject(
                    GetObjectArgs.builder().bucket(bucket).object(objectKey).build()
            );
        } catch (Exception e) {

            throw new StorageException(e.getMessage());
        }
    }

    @Override
    public void delete(String objectKey) {

        try {
            minioClient.removeObject(RemoveObjectArgs.builder().bucket(bucket).object(objectKey).build());
        } catch (Exception e) {

            throw new StorageException("Failed to delete object: " + objectKey);
        }

    }

    @Override
    public void copy(String fromKey, String toKey) {

        try {
           CopySource source = CopySource.builder()
                   .bucket(bucket)
                   .object(fromKey)
                   .build();

           minioClient.copyObject(
                   CopyObjectArgs.builder()
                           .bucket(bucket)
                           .source(source)
                           .object(toKey)
                           .build()
           );
        }
        catch (Exception e) {

            throw new StorageException("Failed to copy object: " + fromKey + " -> " + toKey + ". " + e.getMessage());

        }
    }

    @Override
    public void deleteBatch(Collection<String> objectKeys) {
        if (objectKeys == null || objectKeys.isEmpty()) {
            return;
        }

        List<DeleteObject> objects = objectKeys.stream()
                .map(DeleteObject::new)
                .toList();

        Iterable<Result<DeleteError>> results = minioClient.removeObjects(
                RemoveObjectsArgs.builder()
                        .bucket(bucket)
                        .objects(objects)
                        .build()
        );

        for (Result<DeleteError> result : results) {
            try {
                DeleteError error = result.get();
                throw new StorageException("Failed to delete object: " + error.objectName());
            } catch (Exception e) {
                throw new StorageException("Failed to delete objects: " + e.getMessage());
            }
        }
    }

}
