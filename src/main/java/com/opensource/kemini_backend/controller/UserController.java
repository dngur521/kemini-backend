package com.opensource.kemini_backend.controller;

import com.opensource.kemini_backend.dto.ApiResponse; // 1. ApiResponse import
import com.opensource.kemini_backend.dto.UpdateUserRequestDto;
import com.opensource.kemini_backend.dto.UserResponseDto;
import com.opensource.kemini_backend.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}