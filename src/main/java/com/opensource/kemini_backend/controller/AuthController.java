package com.opensource.kemini_backend.controller;

import com.opensource.kemini_backend.dto.ApiResponse; // 1. ApiResponse import
import com.opensource.kemini_backend.dto.CheckEmailRequestDto;
import com.opensource.kemini_backend.dto.FindEmailRequestDto;
import com.opensource.kemini_backend.dto.SignUpRequestDto;
import com.opensource.kemini_backend.service.UserService;
import com.opensource.kemini_backend.dto.LoginRequestDto;
import com.opensource.kemini_backend.dto.RefreshTokenRequestDto;
import com.opensource.kemini_backend.dto.ResetPasswordByQuestionRequestDto;
import com.opensource.kemini_backend.dto.SecurityQuestionResponseDto;

import software.amazon.awssdk.services.cognitoidentityprovider.model.InitiateAuthResponse;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    // 회원가입 API
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<Void>> signUp(@RequestBody SignUpRequestDto request) {
        userService.signUp(request);
        // 3. ApiResponse.success(메시지)로 래핑
        // return ResponseEntity.ok(ApiResponse.success("회원가입 성공. 이메일로 발송된 코드를 확인해주세요."));
        return ResponseEntity.ok(ApiResponse.success("회원가입 성공. 즉시 로그인할 수 있습니다."));
    }
    
    // 로그인 API
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<Map<String, String>>> login(@RequestBody LoginRequestDto request) {
        InitiateAuthResponse response = userService.login(request);
        
        Map<String, String> tokens = Map.of(
            "id_token", response.authenticationResult().idToken(),
            "access_token", response.authenticationResult().accessToken(),
            "refresh_token", response.authenticationResult().refreshToken()
        );
        return ResponseEntity.ok(ApiResponse.success(tokens, "로그인 성공"));
    }

    // 로그아웃 API
    @PostMapping("/logout")
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

    // refresh token으로 access token 갱신 API (자동로그인 시 사용)
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

    // 아이디(이메일) 찾기 API
    @PostMapping("/find-email")
    public ResponseEntity<ApiResponse<Map<String, String>>> findEmail(
        @RequestBody FindEmailRequestDto request
    ) {
        String email = userService.findEmailByQuestion(request);

        // 이메일 정보를 data에 담아 반환
        return ResponseEntity.ok(ApiResponse.success(
                Map.of("email", email),
                "이메일 찾기에 성공했습니다."));
    }

    // 비밀번호 재설정 API (보안 질문 기반)
    @PostMapping("/reset-password-by-question")
    public ResponseEntity<ApiResponse<Void>> resetPasswordByQuestion(
        @RequestBody ResetPasswordByQuestionRequestDto request
    ) {
        userService.resetPasswordByQuestion(request);

        return ResponseEntity.ok(ApiResponse.success(
                "비밀번호가 성공적으로 재설정되었습니다. 새 비밀번호로 로그인해주세요."));
    }
    
    // 보안 질문 목록 조회 API
    @GetMapping("/questions")
    public ResponseEntity<ApiResponse<List<SecurityQuestionResponseDto>>> getSecurityQuestions() {

        List<SecurityQuestionResponseDto> questions = userService.getSecurityQuestions();

        return ResponseEntity.ok(ApiResponse.success(
                questions,
                "보안 질문 목록 조회에 성공했습니다."));
    }
    
    // 아이디(이메일) 중복 확인 API
    @PostMapping("/check-email")
    public ResponseEntity<ApiResponse<Void>> checkEmailDuplication(
        @RequestBody CheckEmailRequestDto request
    ) {
        // 1. 서비스 호출 (성공 시 메시지 반환, 실패 시 예외 발생)
        String successMessage = userService.checkEmailAvailability(request.email());
        
        // 2. 성공 응답 반환 (ApiResponse.success(메시지))
        return ResponseEntity.ok(ApiResponse.success(successMessage));
    }

}