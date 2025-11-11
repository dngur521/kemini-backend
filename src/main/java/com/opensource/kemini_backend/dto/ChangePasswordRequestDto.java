package com.opensource.kemini_backend.dto;

/**
 * (로그인 상태에서) 비밀번호 변경 요청 DTO
 */
public record ChangePasswordRequestDto(
    String currentPassword, // 현재 비밀번호
    String newPassword      // 새 비밀번호
) {}