package com.opensource.kemini_backend.dto;

import java.util.List;

public record VirtualEnvironmentResponseDto(
    Long id,
    String name,
    Long userId,
    List<EnvironmentFileDto> files // 🚨 단일 URL 대신 파일 리스트 반환
) {}