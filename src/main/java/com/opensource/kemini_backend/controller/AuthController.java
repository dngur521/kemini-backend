package com.opensource.kemini_backend.controller;

import com.opensource.kemini_backend.dto.ApiResponse; // 1. ApiResponse import
import com.opensource.kemini_backend.dto.ConfirmForgotPasswordRequestDto;
import com.opensource.kemini_backend.dto.ConfirmRequestDto;
import com.opensource.kemini_backend.dto.ForgotPasswordRequestDto;
import com.opensource.kemini_backend.dto.SignUpRequestDto;
import com.opensource.kemini_backend.service.UserService;
import com.opensource.kemini_backend.dto.LoginRequestDto;
import com.opensource.kemini_backend.dto.RefreshTokenRequestDto;

import software.amazon.awssdk.services.cognitoidentityprovider.model.InitiateAuthResponse;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/signup")
    // 2. 반환 타입 변경
    public ResponseEntity<ApiResponse<Void>> signUp(@RequestBody SignUpRequestDto request) {
        userService.signUp(request);
        // 3. ApiResponse.success(메시지)로 래핑
        // return ResponseEntity.ok(ApiResponse.success("회원가입 성공. 이메일로 발송된 코드를 확인해주세요."));
        return ResponseEntity.ok(ApiResponse.success("회원가입 성공. 즉시 로그인할 수 있습니다."));
    }
    
    // 11/10~ 사용 안함
    @PostMapping("/confirm")
    // 2. 반환 타입 변경
    public ResponseEntity<ApiResponse<Void>> confirmUser(@RequestBody ConfirmRequestDto request) {
        userService.confirmSignUp(request);
        // 3. ApiResponse.success(메시지)로 래핑
        return ResponseEntity.ok(ApiResponse.success("계정 확인 완료. 이제 로그인할 수 있습니다."));
    }
    
    @PostMapping("/login")
    // 2. 반환 타입 변경 (데이터 타입 T = Map<String, String>)
    public ResponseEntity<ApiResponse<Map<String, String>>> login(@RequestBody LoginRequestDto request) {
        InitiateAuthResponse response = userService.login(request);
        
        Map<String, String> tokens = Map.of(
            "id_token", response.authenticationResult().idToken(),
            "access_token", response.authenticationResult().accessToken(),
            "refresh_token", response.authenticationResult().refreshToken()
        );
        // 3. ApiResponse.success(데이터, 메시지)로 래핑
        return ResponseEntity.ok(ApiResponse.success(tokens, "로그인 성공"));
    }

    @PostMapping("/logout")
    // 2. 반환 타입 변경
    public ResponseEntity<ApiResponse<Void>> logout(
        @RequestHeader("Authorization") String authorizationHeader
    ) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            // 이 예외는 GlobalExceptionHandler가 잡아서 ApiResponse.error()로 변환
            throw new RuntimeException("Authorization 헤더가 없거나 'Bearer ' 접두사가 누락되었습니다.");
        }

        String accessToken = authorizationHeader.substring(7);
        userService.globalSignOut(accessToken);

        // 3. ApiResponse.success(메시지)로 래핑
        return ResponseEntity.ok(ApiResponse.success("로그아웃이 완료되었습니다. 클라이언트의 토큰을 삭제하십시오."));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<Map<String, String>>> refreshToken(
        // 수정된 DTO(email, refreshToken)를 받음
        @RequestBody RefreshTokenRequestDto request
    ) {
        // email과 refreshToken이 포함된 request를 서비스로 전달
        InitiateAuthResponse response = userService.refreshToken(request);

        // 갱신된 토큰 (Access Token, Id Token)
        Map<String, String> tokens = Map.of(
                "id_token", response.authenticationResult().idToken(),
                "access_token", response.authenticationResult().accessToken()
        );

        return ResponseEntity.ok(ApiResponse.success(tokens, "토큰 갱신 성공"));
    }

    /**
     * 비밀번호 재설정 코드 요청 API
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(
        @RequestBody ForgotPasswordRequestDto request
    ) {
        userService.forgotPassword(request);
        
        return ResponseEntity.ok(ApiResponse.success(
            "'" + request.email() + "'로 비밀번호 재설정 코드를 발송했습니다. (이메일 또는 SMS)"
        ));
    }

    /**
     * 비밀번호 재설정 확인 API
     */
    @PostMapping("/confirm-forgot-password")
    public ResponseEntity<ApiResponse<Void>> confirmForgotPassword(
        @RequestBody ConfirmForgotPasswordRequestDto request
    ) {
        userService.confirmForgotPassword(request);

        return ResponseEntity.ok(ApiResponse.success(
                "비밀번호가 성공적으로 재설정되었습니다. 새 비밀번호로 로그인해주세요."));
    }
    
}