package com.opensource.kemini_backend.dto;

// 회원가입 요청 데이터
public record SignUpRequestDto(String email, String password, String name, String phoneNumber) {}
