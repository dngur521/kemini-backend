package com.opensource.kemini_backend.controller;

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

    // 🚨 API Gateway/Nginx를 통해 인증된 사용자만 접근 가능
    @GetMapping("/me")
    public ResponseEntity<UserResponseDto> getMyInfo(@AuthenticationPrincipal String authenticatedEmail) {
        // @AuthenticationPrincipal을 통해 토큰에서 추출된 사용자 이메일을 받음
        UserResponseDto user = userService.getUserInfo(authenticatedEmail);
        return ResponseEntity.ok(user);
    }

    // U: 회원 정보 수정
    @PutMapping("/me")
    public ResponseEntity<UserResponseDto> updateMyInfo(
        @AuthenticationPrincipal String authenticatedEmail,
        @RequestBody UpdateUserRequestDto request
    ) {
        // 토큰에서 추출된 이메일로 사용자 정보를 수정합니다.
        userService.updateUser(authenticatedEmail, request); 
        
        // 수정된 정보를 다시 조회하여 반환합니다.
        UserResponseDto updatedUser = userService.getUserInfo(authenticatedEmail);
        
        return ResponseEntity.ok(updatedUser);
    }

    // D: 회원 탈퇴 (Delete)
    @DeleteMapping("/me")
    public ResponseEntity<String> deleteMyAccount(
        @AuthenticationPrincipal String authenticatedEmail
    ) {
        // 토큰에서 추출된 이메일로 계정 삭제를 요청합니다.
        userService.deleteUser(authenticatedEmail);
        
        return ResponseEntity.ok("계정(" + authenticatedEmail + ")이 성공적으로 삭제되었습니다.");
    }
}