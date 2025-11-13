package com.opensource.kemini_backend.dto;

public record S3PresignedUrlResponseDto(
    String presignedUploadUrl,
    String finalFileUrl
) {}