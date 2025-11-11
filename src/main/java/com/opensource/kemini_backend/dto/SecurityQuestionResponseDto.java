package com.opensource.kemini_backend.dto;

/**
 * 보안 질문 목록 응답 DTO
 */
public record SecurityQuestionResponseDto(
    Long id,
    String questionText
) {}