package com.opensource.kemini_backend.dto;


/*
 * 회원가입 요청 DTO
 */
public record SignUpRequestDto(
        String email,
        String password,   
        String name,
        String phoneNumber,
        Long askId,
        String askAnswer
) {}
