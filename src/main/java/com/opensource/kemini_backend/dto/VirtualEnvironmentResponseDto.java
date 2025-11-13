package com.opensource.kemini_backend.dto;

import com.opensource.kemini_backend.model.VirtualEnvironment;

/**
 * 가상환경 생성/조회 시 서버가 반환할 DTO
 */
public record VirtualEnvironmentResponseDto(
    Long id,
    String name,
    String s3FileUrl, // 1. 🚨 s3ObjectKey -> s3FileUrl로 변경
    Long userId
) {
    // 2. 🚨 생성자 삭제 (엔티티를 직접 받지 않고, Service에서 매핑해서 생성)
}