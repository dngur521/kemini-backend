package com.opensource.kemini_backend.controller;

import com.opensource.kemini_backend.dto.ApiResponse; // 1. ApiResponse import
import com.opensource.kemini_backend.dto.ChangePasswordRequestDto;
import com.opensource.kemini_backend.dto.UpdateUserRequestDto;
import com.opensource.kemini_backend.dto.UserResponseDto;
import com.opensource.kemini_backend.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import com.opensource.kemini_backend.filter.CognitoHeaderAuthenticationFilter;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    // 2. 반환 타입 변경 (데이터 타입 T = UserResponseDto)
    public ResponseEntity<ApiResponse<UserResponseDto>> getMyInfo(@AuthenticationPrincipal String authenticatedEmail) {
        UserResponseDto user = userService.getUserInfo(authenticatedEmail);
        // 3. ApiResponse.success(데이터, 메시지)로 래핑
        return ResponseEntity.ok(ApiResponse.success(user, "사용자 정보 조회 성공"));
    }

    @PutMapping("/me")
    // 2. 반환 타입 변경
    public ResponseEntity<ApiResponse<UserResponseDto>> updateMyInfo(
        @AuthenticationPrincipal String authenticatedEmail,
        @RequestBody UpdateUserRequestDto request
    ) {
        userService.updateUser(authenticatedEmail, request); 
        UserResponseDto updatedUser = userService.getUserInfo(authenticatedEmail);
        
        // 3. ApiResponse.success(데이터, 메시지)로 래핑
        return ResponseEntity.ok(ApiResponse.success(updatedUser, "사용자 정보 수정 성공"));
    }

    @DeleteMapping("/me")
    // 2. 반환 타입 변경
    public ResponseEntity<ApiResponse<Void>> deleteMyAccount(
        @AuthenticationPrincipal String authenticatedEmail
    ) {
        userService.deleteUser(authenticatedEmail);
        String message = String.format("계정(%s)이 성공적으로 삭제되었습니다.", authenticatedEmail);
        
        // 3. ApiResponse.success(메시지)로 래핑
        return ResponseEntity.ok(ApiResponse.success(message));
    }

    /**
     * 로그인 상태에서 비밀번호 변경
     */
    @PostMapping("/me/change-password")
    public ResponseEntity<ApiResponse<Void>> changeMyPassword(
        // 5. 🚨 필터가 검증한 토큰을 다시 가져옴
        @RequestHeader(CognitoHeaderAuthenticationFilter.AUTH_HEADER_KEY) String authorizationHeader,
        @RequestBody ChangePasswordRequestDto request
    ) {
        // 6. "Bearer " 접두사 제거
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new RuntimeException("Authorization 헤더가 없거나 'Bearer ' 접두사가 누락되었습니다.");
        }
        String accessToken = authorizationHeader.substring(7);

        // 7. 서비스 로직 호출 (access_token 전달)
        userService.changePassword(accessToken, request);

        return ResponseEntity.ok(ApiResponse.success("비밀번호가 성공적으로 변경되었습니다."));
    }
}