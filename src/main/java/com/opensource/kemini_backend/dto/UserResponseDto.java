package com.opensource.kemini_backend.dto;

/**
 * 사용자 정보 응답 DTO (비밀번호 제외)
 */
public record UserResponseDto(
    String email, 
    String name, 
    String phoneNumber, 
    String status
) {}
