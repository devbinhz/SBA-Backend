package com.bookverse.controller;

import com.bookverse.common.dto.ApiResponse;
import com.bookverse.service.storage.MinioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/uploads")
@Tag(name = "Book", description = "Book Management APIs")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class AdminUploadController {

    private final MinioService minioService;

    public AdminUploadController(MinioService minioService) {
        this.minioService = minioService;
    }

    @PostMapping("/book-file")
    @Operation(summary = "Upload PDF/EPUB book file")
    public ApiResponse<Map<String, String>> uploadBookFile(@RequestParam("file") MultipartFile file) throws IOException {
        String filename = file.getOriginalFilename();
        if (filename == null || filename.isBlank()) {
            throw new IllegalArgumentException("Invalid file name");
        }
        minioService.uploadBook(filename, file.getInputStream(), file.getSize(), file.getContentType());
        return ApiResponse.success(Map.of("fileKey", filename));
    }

    @PostMapping("/thumbnail")
    @Operation(summary = "Upload book cover thumbnail")
    public ApiResponse<Map<String, String>> uploadThumbnail(@RequestParam("file") MultipartFile file) throws IOException {
        String filename = file.getOriginalFilename();
        if (filename == null || filename.isBlank()) {
            throw new IllegalArgumentException("Invalid file name");
        }
        minioService.uploadThumbnail(filename, file.getInputStream(), file.getSize(), file.getContentType());
        return ApiResponse.success(Map.of("coverKey", filename));
    }
}
