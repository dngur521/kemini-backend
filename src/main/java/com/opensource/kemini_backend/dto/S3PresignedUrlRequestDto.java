package com.opensource.kemini_backend.dto;

public record S3PresignedUrlRequestDto(
    String fileName,
    String fileType // 🚨 추가됨 ("SPACE" or "MARKER")
) {}