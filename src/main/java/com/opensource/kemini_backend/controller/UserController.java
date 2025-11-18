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

    // 내 정보 조회 API
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponseDto>> getMyInfo(@AuthenticationPrincipal String authenticatedEmail) {
        UserResponseDto user = userService.getUserInfo(authenticatedEmail);
        return ResponseEntity.ok(ApiResponse.success(user, "사용자 정보 조회 성공"));
    }

    // 내 정보 수정 API
    @PutMapping("/me")
    public ResponseEntity<ApiResponse<UserResponseDto>> updateMyInfo(
        @AuthenticationPrincipal String authenticatedEmail,
        @RequestBody UpdateUserRequestDto request
    ) {
        userService.updateUser(authenticatedEmail, request); 
        UserResponseDto updatedUser = userService.getUserInfo(authenticatedEmail);
        
        return ResponseEntity.ok(ApiResponse.success(updatedUser, "사용자 정보 수정 성공"));
    }

    // 회원 탈퇴 API
    @DeleteMapping("/me")
    public ResponseEntity<ApiResponse<Void>> deleteMyAccount(
        @AuthenticationPrincipal String authenticatedEmail
    ) {
        userService.deleteUser(authenticatedEmail);
        String message = String.format("계정(%s)이 성공적으로 삭제되었습니다.", authenticatedEmail);
        
        return ResponseEntity.ok(ApiResponse.success(message));
    }

    // 비밀번호 변경하는 API
    @PostMapping("/me/change-password")
    public ResponseEntity<ApiResponse<Void>> changeMyPassword(
        @RequestHeader(CognitoHeaderAuthenticationFilter.AUTH_HEADER_KEY) String authorizationHeader,
        @RequestBody ChangePasswordRequestDto request
    ) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new RuntimeException("Authorization 헤더가 없거나 'Bearer ' 접두사가 누락되었습니다.");
        }
        String accessToken = authorizationHeader.substring(7);

        userService.changePassword(accessToken, request);

        return ResponseEntity.ok(ApiResponse.success("비밀번호가 성공적으로 변경되었습니다."));
    }
}