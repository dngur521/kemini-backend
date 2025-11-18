package com.opensource.kemini_backend.dto;

/*
 * S3에 파일 업로드할때 Url 응답 DTO
 */
public record S3PresignedUrlResponseDto(
    String presignedUploadUrl,
    String finalFileUrl
) {}