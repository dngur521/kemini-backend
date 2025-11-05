package com.opensource.kemini_backend.controller;

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
    public ResponseEntity<String> signUp(@RequestBody SignUpRequestDto request) {
        userService.signUp(request);
        return ResponseEntity.ok("회원가입 성공. 이메일로 발송된 코드를 확인해주세요.");
    }
    
    @PostMapping("/confirm")
    public ResponseEntity<String> confirmUser(@RequestBody ConfirmRequestDto request) {
        userService.confirmSignUp(request);
        return ResponseEntity.ok("계정 확인 완료. 이제 로그인할 수 있습니다.");
    }
    
    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@RequestBody LoginRequestDto request) {
        InitiateAuthResponse response = userService.login(request);
        
        // 토큰 정보를 클라이언트에게 반환합니다.
        // 클라이언트는 이 토큰을 저장하고 향후 API 호출 시 사용합니다.
        Map<String, String> tokens = Map.of(
            "id_token", response.authenticationResult().idToken(),
            "access_token", response.authenticationResult().accessToken(),
            "refresh_token", response.authenticationResult().refreshToken()
        );
        return ResponseEntity.ok(tokens);
    }

    // D: 로그아웃 (토큰 무효화)
    @PostMapping("/logout")
    public ResponseEntity<String> logout(
        @RequestHeader("Authorization") String authorizationHeader
    ) {
        // 1. Authorization 헤더에서 "Bearer " 접두사를 제거하여 토큰 추출
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return ResponseEntity.badRequest().body("Authorization header missing or invalid.");
        }

        String accessToken = authorizationHeader.substring(7);

        // 2. 서비스 로직 호출 (토큰 무효화)
        userService.globalSignOut(accessToken);

        // 3. 응답: 200 OK (클라이언트에게 토큰을 삭제하도록 유도)
        return ResponseEntity.ok("로그아웃이 완료되었습니다. 클라이언트의 토큰을 삭제하십시오.");
    }

}

