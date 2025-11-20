package com.opensource.kemini_backend.dto;

/**
 * 보안 질문을 통한 비밀번호 재설정 요청 DTO
 */
public record ResetPasswordByQuestionRequestDto(
    String email,
    Long askId,
    String askAnswer,
    String newPassword
) {}