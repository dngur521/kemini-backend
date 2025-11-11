package com.opensource.kemini_backend.dto;

/**
 * 아이디(이메일) 찾기 요청 DTO
 */
public record FindEmailRequestDto(
    String phoneNumber,
    Long askId,
    String askAnswer
) {}