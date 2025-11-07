package com.opensource.kemini_backend.controller;

import com.opensource.kemini_backend.dto.ApiResponse; // 1. ApiResponse import
import com.opensource.kemini_backend.dto.ConfirmRequestDto;
import com.opensource.kemini_backend.dto.SignUpRequestDto;
import com.opensource.kemini_backend.service.UserService;
import com.opensource.kemini_backend.dto.LoginRequestDto;

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
        return ResponseEntity.ok(ApiResponse.success("회원가입 성공. 이메일로 발송된 코드를 확인해주세요."));
    }
    
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
}