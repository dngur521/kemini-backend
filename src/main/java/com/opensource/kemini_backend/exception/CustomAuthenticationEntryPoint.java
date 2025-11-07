package com.opensource.kemini_backend.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opensource.kemini_backend.dto.ApiResponse;
import com.opensource.kemini_backend.dto.ErrorResponse;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Spring Security에서 인증되지 않은 사용자의 접근(401)을 처리하는
 * 커스텀 진입 지점입니다. (ApiResponse 형식으로 반환)
 */
@Component // 1. Spring Bean으로 등록
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void commence(
        HttpServletRequest request,
        HttpServletResponse response,
        AuthenticationException authException
    ) throws IOException, ServletException {
        
        // 2. 사용자가 보던 바로 그 메시지를 사용
        String message = "인증이 필요합니다. 유효한 토큰을 포함하여 요청하십시오.";
        ErrorResponse errorResponse = new ErrorResponse("UNAUTHORIZED", message);
        
        // 3. ApiResponse로 래핑 (표준 응답)
        ApiResponse<Void> apiResponse = ApiResponse.error(errorResponse);

        // 4. Filter에서 했던 것과 동일하게 JSON 응답을 직접 작성
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        String jsonResponse = objectMapper.writeValueAsString(apiResponse);
        response.getWriter().write(jsonResponse);
    }
}