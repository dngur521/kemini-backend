package com.opensource.kemini_backend.dto;

/**
 * 비밀번호 재설정 확인 DTO
 */
public record ConfirmForgotPasswordRequestDto(
    String email, 
    String confirmationCode, 
    String newPassword
) {}