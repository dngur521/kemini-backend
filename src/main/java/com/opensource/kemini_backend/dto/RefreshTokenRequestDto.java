package com.opensource.kemini_backend.dto;

/*
 * 토큰 요청 DTO
 */
public record RefreshTokenRequestDto(String email, String refreshToken) {}