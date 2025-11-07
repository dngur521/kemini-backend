package com.opensource.kemini_backend.dto;

/**
 * 전역 예외 처리를 위한 표준 오류 응답 DTO
 * @param errorCode (커스텀 오류 코드)
 * @param message (예외 메시지)
 */
public record ErrorResponse(String errorCode, String message) {}