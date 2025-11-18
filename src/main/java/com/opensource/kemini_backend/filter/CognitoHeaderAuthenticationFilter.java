package com.opensource.kemini_backend.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opensource.kemini_backend.dto.ApiResponse;
import com.opensource.kemini_backend.dto.ErrorResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.filter.OncePerRequestFilter;

import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.GetUserRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.NotAuthorizedException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import org.json.JSONObject;

public class CognitoHeaderAuthenticationFilter extends OncePerRequestFilter {

    public static final String AUTH_HEADER_KEY = "X-Authenticated-User-Email";
    private final CognitoIdentityProviderClient cognitoClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public CognitoHeaderAuthenticationFilter(CognitoIdentityProviderClient cognitoClient) {
        this.cognitoClient = cognitoClient;
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {

        String fullTokenHeader = request.getHeader(AUTH_HEADER_KEY);
        String authenticatedEmail = null;

        if (fullTokenHeader != null && fullTokenHeader.startsWith("Bearer ")) {
            String token = fullTokenHeader.substring(7);

            if (!isTokenValidOnline(token)) {
                logger.warn("Online token validation failed (Signed out or expired). Returning 401.");
                
                sendErrorResponse(response, HttpStatus.UNAUTHORIZED, "TOKEN_INVALID", "토큰이 유효하지 않거나 만료되었습니다. 다시 로그인해주세요.");
                return; 
            }

            try {
                String[] parts = token.split("\\.");
                if (parts.length > 1) {
                    byte[] decodedBytes = Base64.getUrlDecoder().decode(parts[1]);
                    String payload = new String(decodedBytes, StandardCharsets.UTF_8);
                    JSONObject jsonPayload = new JSONObject(payload);
                    authenticatedEmail = jsonPayload.optString("username", null);
                    System.out.println("DEBUG: Successfully parsed email from token: " + authenticatedEmail);
                }
            } catch (Exception e) {
                logger.warn("JWT payload parsing error: " + e.getMessage());
            }
        } else {
            logger.warn("DEBUG: Header '" + AUTH_HEADER_KEY + "' is null or doesn't start with Bearer.");
        }

        if (authenticatedEmail != null) {
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    authenticatedEmail,
                    null,
                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }
    
    private boolean isTokenValidOnline(String accessToken) {
        try {
            GetUserRequest getUserRequest = GetUserRequest.builder().accessToken(accessToken).build();
            cognitoClient.getUser(getUserRequest);
            return true;
        } catch (NotAuthorizedException e) {
            return false;
        } catch (Exception e) {
            logger.error("Error during Cognito GetUser check: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 필터에서 JSON 형식의 오류 응답을 직접 생성하는 헬퍼 메서드
     * (ApiResponse를 반환하도록 수정)
     */
    private void sendErrorResponse(HttpServletResponse response, HttpStatus status, String errorCode, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        ErrorResponse errorResponse = new ErrorResponse(errorCode, message);
        
        ApiResponse<Void> apiResponse = ApiResponse.error(errorResponse);
        
        String jsonResponse = objectMapper.writeValueAsString(apiResponse);

        response.getWriter().write(jsonResponse);
    }
}