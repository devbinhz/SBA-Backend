package com.bookverse.service.storage.impl;

import com.bookverse.config.MinioProperties;
import com.bookverse.service.storage.MinioService;
import io.minio.BucketExistsArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.StatObjectArgs;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.http.Method;
import org.springframework.stereotype.Service;
import java.io.InputStream;

@Service
public class MinioServiceImpl implements MinioService {

    private final MinioClient minioClient;
    private final MinioProperties minioProperties;

    public MinioServiceImpl(MinioClient minioClient, MinioProperties minioProperties) {
        this.minioClient = minioClient;
        this.minioProperties = minioProperties;
    }

    @Override
    public String getPresignedBookUrl(String fileName, int expirySeconds) {
        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(minioProperties.booksBucket())
                            .object(fileName)
                            .expiry(expirySeconds)
                            .build()
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to get presigned URL for book: " + fileName, e);
        }
    }

    @Override
    public boolean bookExists(String fileName) {
        try {
            minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(minioProperties.booksBucket())
                            .object(fileName)
                            .build()
            );
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean isAvailable() {
        try {
            return minioClient.bucketExists(
                    BucketExistsArgs.builder()
                            .bucket(minioProperties.booksBucket())
                            .build()
            );
        } catch (Exception e) {
            return false;
        }
    }
    @Override
    public String getPresignedThumbnailUrl(String fileName, int expirySeconds) {
        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(minioProperties.thumbnailsBucket())
                            .object(fileName)
                            .expiry(expirySeconds)
                            .build()
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to get presigned URL for thumbnail: " + fileName, e);
        }
    }

    @Override
    public String uploadBook(String objectKey, InputStream data, long size, String contentType) {
        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(minioProperties.booksBucket())
                            .object(objectKey)
                            .stream(data, size, -1)
                            .contentType(contentType)
                            .build()
            );
            return objectKey;
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload book to MinIO: " + objectKey, e);
        }
    }

    @Override
    public String uploadThumbnail(String objectKey, InputStream data, long size, String contentType) {
        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(minioProperties.thumbnailsBucket())
                            .object(objectKey)
                            .stream(data, size, -1)
                            .contentType(contentType)
                            .build()
            );
            return objectKey;
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload thumbnail to MinIO: " + objectKey, e);
        }
    }

    @Override
    public void deleteBook(String objectKey) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(minioProperties.booksBucket())
                            .object(objectKey)
                            .build()
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete book from MinIO: " + objectKey, e);
        }
    }

    @Override
    public void deleteThumbnail(String objectKey) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(minioProperties.thumbnailsBucket())
                            .object(objectKey)
                            .build()
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete thumbnail from MinIO: " + objectKey, e);
        }
    }
}
