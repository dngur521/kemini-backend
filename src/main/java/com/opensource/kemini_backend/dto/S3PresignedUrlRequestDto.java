package com.opensource.kemini_backend.dto;

/*
 * S3에 파일 업로드 요청 DTO
 */
public record S3PresignedUrlRequestDto(
    String fileName
) {}