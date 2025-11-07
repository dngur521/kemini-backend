package com.opensource.kemini_backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.opensource.kemini_backend.filter.CognitoHeaderAuthenticationFilter;
import com.opensource.kemini_backend.exception.CustomAccessDeniedHandler;
import com.opensource.kemini_backend.exception.CustomAuthenticationEntryPoint;

import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    // 1. CognitoClient를 주입받기 위한 필드 및 생성자 추가
    private final CognitoIdentityProviderClient cognitoClient;
    private final CustomAuthenticationEntryPoint customAuthenticationEntryPoint;
    private final CustomAccessDeniedHandler customAccessDeniedHandler;

    public SecurityConfig(
        CognitoIdentityProviderClient cognitoClient,
        CustomAuthenticationEntryPoint customAuthenticationEntryPoint,
        CustomAccessDeniedHandler customAccessDeniedHandler ) {
        this.cognitoClient = cognitoClient;
        this.customAuthenticationEntryPoint = customAuthenticationEntryPoint;
        this.customAccessDeniedHandler = customAccessDeniedHandler;
    }

    // 2. Filter를 Bean으로 정의하고 CognitoClient 주입
    @Bean
    public CognitoHeaderAuthenticationFilter cognitoHeaderAuthenticationFilter() {
        return new CognitoHeaderAuthenticationFilter(cognitoClient);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        // 1. CORS, CSRF, 세션 비활성화
        http
            .csrf(AbstractHttpConfigurer::disable)   // REST API이므로 CSRF 비활성화
            .cors(AbstractHttpConfigurer::disable)   // CORS는 보통 Gateway/Nginx에서 처리
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)); // JWT 기반 인증이므로 세션 비활성화

        // 2. 권한 설정
        http
            .authorizeHttpRequests(auth -> auth
                // 회원가입, 로그인, 확인 경로는 인증 없이 접근 허용
                .requestMatchers("/api/v1/auth/**").permitAll() 
                
                // 그 외 모든 경로는 인증 필수
                .anyRequest().authenticated()
            );

        // 3. 커스텀 필터 등록 (수정된 부분) 👈
        http
            // Bean으로 등록된 필터를 호출하여 Cognito Client가 주입된 상태로 사용
            .addFilterBefore(
                cognitoHeaderAuthenticationFilter(), 
                UsernamePasswordAuthenticationFilter.class
            );

        // 4. 기존 Spring Security의 기본 로그인 페이지 비활성화 (이전 문제에서 본 페이지)
        http
            .formLogin(AbstractHttpConfigurer::disable);

        // 5. 🚨 예외 처리 핸들러 등록
        http
            .exceptionHandling(ex -> ex
                // 401 (Unauthorized) 처리
                .authenticationEntryPoint(customAuthenticationEntryPoint)
                // 403 (Forbidden) 처리
                .accessDeniedHandler(customAccessDeniedHandler)
            );
        return http.build();
    }
}