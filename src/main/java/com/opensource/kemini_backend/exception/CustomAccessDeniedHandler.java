package com.opensource.kemini_backend.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opensource.kemini_backend.dto.ErrorResponse;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 인증은 되었으나 권한이 없는 사용자(403)의 접근 시도를 처리하는 핸들러
 */
@Component
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException) throws IOException, ServletException {
        
        ErrorResponse errorResponse = new ErrorResponse(
                "FORBIDDEN",
                "이 리소스에 접근할 권한이 없습니다."
        );

        response.setStatus(HttpServletResponse.SC_FORBIDDEN); // 403 상태 코드
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}