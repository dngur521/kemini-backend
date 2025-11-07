package com.opensource.kemini_backend.exception;

import com.opensource.kemini_backend.dto.ApiResponse;
import com.opensource.kemini_backend.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import software.amazon.awssdk.services.cognitoidentityprovider.model.CognitoIdentityProviderException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * @RequestHeader 등 필수 헤더 누락 시 처리
     * (반환 타입을 ApiResponse<Void>로 변경)
     */
    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingRequestHeader(MissingRequestHeaderException e) {
        String message = String.format("필수 헤더 '%s'가 누락되었습니다.", e.getHeaderName());
        ErrorResponse error = new ErrorResponse("HEADER_MISSING", message);
        
        // ApiResponse.error()로 감싸서 반환
        return new ResponseEntity<>(ApiResponse.error(error), HttpStatus.BAD_REQUEST);
    }

    /**
     * UserService에서 발생하는 대부분의 런타임 예외 처리
     * (반환 타입을 ApiResponse<Void>로 변경)
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse<Void>> handleRuntimeException(RuntimeException e) {
        ErrorResponse error = new ErrorResponse("RUNTIME_ERROR", e.getMessage());
        return new ResponseEntity<>(ApiResponse.error(error), HttpStatus.BAD_REQUEST);
    }

    /**
     * Cognito SDK에서 발생하는 예외 처리
     * (반환 타입을 ApiResponse<Void>로 변경)
     */
    @ExceptionHandler(CognitoIdentityProviderException.class)
    public ResponseEntity<ApiResponse<Void>> handleCognitoException(CognitoIdentityProviderException e) {
        ErrorResponse error = new ErrorResponse("COGNITO_ERROR", e.awsErrorDetails().errorMessage());
        HttpStatus status = HttpStatus.valueOf(e.statusCode());
        return new ResponseEntity<>(ApiResponse.error(error), status);
    }

    /**
     * 위에서 잡지 못한 나머지 모든 예외 처리
     * (반환 타입을 ApiResponse<Void>로 변경)
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneralException(Exception e) {
        ErrorResponse error = new ErrorResponse("INTERNAL_ERROR", "서버 내부 오류가 발생했습니다: " + e.getMessage());
        return new ResponseEntity<>(ApiResponse.error(error), HttpStatus.INTERNAL_SERVER_ERROR);
    }
}