package com.bookverse.service.storage;

import java.io.InputStream;

public interface MinioService {
    String getPresignedBookUrl(String fileName, int expirySeconds);
    String getPresignedThumbnailUrl(String fileName, int expirySeconds);
    boolean bookExists(String fileName);
    boolean isAvailable();
    String uploadBook(String objectKey, InputStream data, long size, String contentType);
    String uploadThumbnail(String objectKey, InputStream data, long size, String contentType);
    void deleteBook(String objectKey);
    void deleteThumbnail(String objectKey);
}
