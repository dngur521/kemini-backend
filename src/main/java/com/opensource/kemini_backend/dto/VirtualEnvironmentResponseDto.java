package com.opensource.kemini_backend.dto;

/**
 * 가상환경 생성/조회 응답 DTO
 */
public record VirtualEnvironmentResponseDto(
    Long id,
    String name,
    String s3FileUrl,
    Long userId
) {
}