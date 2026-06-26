package com.bookverse.service.storage;

public interface MinioService {
    String getPresignedBookUrl(String fileName, int expirySeconds);
    boolean bookExists(String fileName);
    boolean isAvailable();
}
