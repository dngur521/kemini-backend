package com.opensource.kemini_backend.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.filter.OncePerRequestFilter;

import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.GetUserRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.NotAuthorizedException;

// JWT 파싱을 위한 import
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import org.json.JSONObject; // org.json:json 의존성 필요

/**
 * API Gateway/Nginx 등에서 인증 후 전달된 HTTP 헤더를 기반으로 
 * Spring Security Context에 인증 정보를 주입하는 필터입니다.
 */
public class CognitoHeaderAuthenticationFilter extends OncePerRequestFilter {

    // Nginx가 토큰 전체를 담아 전달하는 헤더 이름
    public static final String AUTH_HEADER_KEY = "X-Authenticated-User-Email";

    // Cognito Client 주입을 위한 생성자 추가
    private final CognitoIdentityProviderClient cognitoClient;

    public CognitoHeaderAuthenticationFilter(CognitoIdentityProviderClient cognitoClient) {
        this.cognitoClient = cognitoClient;
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {

        // 1. Nginx로부터 토큰 헤더(X-Authenticated-User-Email)를 받음
        String fullTokenHeader = request.getHeader(AUTH_HEADER_KEY);
        String authenticatedEmail = null;

        if (fullTokenHeader != null && fullTokenHeader.startsWith("Bearer ")) {
            // "Bearer " 접두사 제거
            String token = fullTokenHeader.substring(7);

            // ⭐️ 1단계: Cognito 서버에 토큰이 유효한지 온라인으로 확인 (로그아웃되었는지 즉시 확인)
            if (!isTokenValidOnline(token)) {
                logger.warn("Online token validation failed (Signed out or expired). Returning 401.");
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token is invalid or logged out."); // 401 반환
                return; // 요청 처리 중단
            }

            try {
                // 2. JWT는 Header.Payload.Signature로 구성됨.
                // 우리는 두 번째 부분(Payload)만 필요.
                String[] parts = token.split("\\.");

                if (parts.length > 1) {
                    // 3. Payload 부분을 Base64 URL-safe 디코딩
                    byte[] decodedBytes = Base64.getUrlDecoder().decode(parts[1]);
                    String payload = new String(decodedBytes, StandardCharsets.UTF_8);

                    // 4. JSON으로 파싱
                    JSONObject jsonPayload = new JSONObject(payload);

                    // 5. Cognito Access Token의 'username' 클레임 추출 (이것이 이메일)
                    authenticatedEmail = jsonPayload.optString("username", null);

                    // (디버그 로그)
                    System.out.println("DEBUG: Successfully parsed email from token: " + authenticatedEmail);
                }
            } catch (Exception e) {
                // 토큰 파싱 또는 디코딩 오류
                logger.warn("JWT payload parsing error: " + e.getMessage());
            }
        } else {
            logger.warn("DEBUG: Header '" + AUTH_HEADER_KEY + "' is null or doesn't start with Bearer.");
        }

        // 6. 추출된 이메일이 있으면 Security Context에 인증 정보 주입
        if (authenticatedEmail != null) {
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    authenticatedEmail, // Principal (이메일)
                    null,
                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }
    
    // ⭐️ 토큰 유효성 온라인 체크 메서드
    private boolean isTokenValidOnline(String accessToken) {
        try {
            // GetUser API 호출: 토큰이 취소(로그아웃)되면 NotAuthorizedException 발생
            GetUserRequest getUserRequest = GetUserRequest.builder().accessToken(accessToken).build();
            cognitoClient.getUser(getUserRequest);
            return true;
        } catch (NotAuthorizedException e) {
            // 로그아웃, 만료 등으로 인해 토큰이 유효하지 않음
            return false;
        } catch (Exception e) {
            logger.error("Error during Cognito GetUser check: " + e.getMessage());
            return false;
        }
    }
}