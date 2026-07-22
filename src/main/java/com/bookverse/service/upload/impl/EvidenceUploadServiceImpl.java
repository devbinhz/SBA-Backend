package com.bookverse.service.upload.impl;

import com.bookverse.common.exception.BadRequestException;
import com.bookverse.config.MinioProperties;
import com.bookverse.service.storage.MinioService;
import com.bookverse.service.upload.EvidenceUploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EvidenceUploadServiceImpl implements EvidenceUploadService {

    private static final Set<String> ALLOWED_CONTENT_PREFIXES = Set.of("image/", "video/");
    private static final long MAX_FILE_SIZE_BYTES = 20L * 1024 * 1024;

    private final MinioService minioService;
    private final MinioProperties minioProperties;

    @Override
    public String upload(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new BadRequestException("File is empty");
        }
        String contentType = file.getContentType();
        if (contentType == null || ALLOWED_CONTENT_PREFIXES.stream().noneMatch(contentType::startsWith)) {
            throw new BadRequestException("Only image or video files are allowed");
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new BadRequestException("File size must not exceed 20MB");
        }

        String originalFilename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "file";
        String sanitizedFilename = originalFilename.replaceAll("[^a-zA-Z0-9._-]", "_");
        String objectKey = "evidence/" + UUID.randomUUID() + "_" + sanitizedFilename;

        minioService.uploadThumbnail(objectKey, file.getInputStream(), file.getSize(), contentType);

        return minioProperties.publicEndpoint() + "/" + minioProperties.thumbnailsBucket() + "/" + objectKey;
    }
}
