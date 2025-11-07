package com.opensource.kemini_backend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;

/**
 * 모든 API 응답을 위한 표준 래퍼 DTO
 * @param <T> 응답 데이터의 타입
 */
@Getter
// 1. JSON 직렬화 시 null인 필드는 아예 제외시킵니다.
@JsonInclude(JsonInclude.Include.NON_NULL) 
public class ApiResponse<T> {

    private final String status; // "success" 또는 "error"
    private final String message;
    private final T data; // 2. 성공 시 데이터
    private final ErrorResponse error; // 3. 실패 시 에러 정보

    // 성공 응답 생성자
    private ApiResponse(String status, String message, T data) {
        this.status = status;
        this.message = message;
        this.data = data;
        this.error = null;
    }

    // 실패 응답 생성자
    private ApiResponse(String status, ErrorResponse error) {
        this.status = status;
        this.message = error.message(); // 에러 메시지를 공통 메시지 필드로도 사용
        this.data = null;
        this.error = error;
    }

    // --- 정적 팩토리 메서드 (사용 편의를 위함) ---

    /**
     * 성공 (데이터 + 커스텀 메시지)
     */
    public static <T> ApiResponse<T> success(T data, String message) {
        return new ApiResponse<>("success", message, data);
    }

    /**
     * 성공 (데이터만, 기본 메시지)
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>("success", "요청에 성공했습니다.", data);
    }
    
    /**
     * 성공 (메시지만, 데이터 없음)
     */
    public static ApiResponse<Void> success(String message) {
        return new ApiResponse<>("success", message, null);
    }

    /**
     * 실패 (ErrorResponse 객체 사용)
     */
    public static ApiResponse<Void> error(ErrorResponse error) {
        return new ApiResponse<>("error", error);
    }
}