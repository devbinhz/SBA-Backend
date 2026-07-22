package com.bookverse.service.upload;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface EvidenceUploadService {
    String upload(MultipartFile file) throws IOException;
}
