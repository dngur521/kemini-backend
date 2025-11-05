package com.opensource.kemini_backend.dto;

// 계정 확인 요청 데이터
public record ConfirmRequestDto(String email, String confirmationCode) {}
