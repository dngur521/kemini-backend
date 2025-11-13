package com.opensource.kemini_backend.controller;

import com.opensource.kemini_backend.dto.*;
import com.opensource.kemini_backend.service.EnvironmentService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/environments") // (URL 변경: /files -> /environments)
public class EnvironmentController {

    private final EnvironmentService environmentService;

    public EnvironmentController(EnvironmentService environmentService) {
        this.environmentService = environmentService;
    }

    /**
     * API 1: 새 가상환경 생성
     * (이 API는 자동으로 인증이 필요합니다)
     */
    @PostMapping
    public ResponseEntity<ApiResponse<VirtualEnvironmentResponseDto>> createEnvironment(
            @AuthenticationPrincipal String authenticatedEmail,
            @RequestBody VirtualEnvironmentRequestDto request
    ) {
        VirtualEnvironmentResponseDto responseDto = environmentService.createEnvironment(
            authenticatedEmail, 
            request
        );
        return ResponseEntity.ok(ApiResponse.success(responseDto, "가상환경이 생성되었습니다."));
    }

    /**
     * API 2: 파일 업로드 URL 요청
     * (이 API는 자동으로 인증이 필요합니다)
     */
    @PostMapping("/{envId}/request-upload")
    public ResponseEntity<ApiResponse<S3PresignedUrlResponseDto>> requestUploadUrl(
            @AuthenticationPrincipal String authenticatedEmail,
            @PathVariable("envId") Long envId,
            @RequestBody S3PresignedUrlRequestDto request
    ) {
        S3PresignedUrlResponseDto responseDto = environmentService.generateUploadUrl(
            authenticatedEmail, 
            envId, 
            request
        );
        return ResponseEntity.ok(ApiResponse.success(responseDto, "업로드 URL이 생성되었습니다."));
    }

    /**
     * 3. 🚨 (새로 추가) [Load List] 내 모든 가상환경 조회
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<VirtualEnvironmentResponseDto>>> getAllMyEnvironments(
            @AuthenticationPrincipal String authenticatedEmail
    ) {
        List<VirtualEnvironmentResponseDto> responseDtoList = 
            environmentService.getAllEnvironments(authenticatedEmail);
        
        return ResponseEntity.ok(ApiResponse.success(
            responseDtoList, 
            "가상환경 목록 조회에 성공했습니다."
        ));
    }

    /**
     * 4. 🚨 (새로 추가) [Load Single] 특정 가상환경 상세 조회
     */
    @GetMapping("/{envId}")
    public ResponseEntity<ApiResponse<VirtualEnvironmentResponseDto>> getEnvironmentById(
            @AuthenticationPrincipal String authenticatedEmail,
            @PathVariable("envId") Long envId
    ) {
        VirtualEnvironmentResponseDto responseDto = 
            environmentService.getEnvironmentById(authenticatedEmail, envId);
        
        return ResponseEntity.ok(ApiResponse.success(
            responseDto, 
            "가상환경 상세 조회에 성공했습니다."
        ));
    }

    /**
     * 5. 🚨 (새로 추가) [Update Name] 가상환경 이름 수정
     */
    @PutMapping("/{envId}")
    public ResponseEntity<ApiResponse<VirtualEnvironmentResponseDto>> updateEnvironmentName(
            @AuthenticationPrincipal String authenticatedEmail,
            @PathVariable("envId") Long envId,
            @RequestBody VirtualEnvironmentRequestDto request // (이름 수정을 위해 재사용)
    ) {
        VirtualEnvironmentResponseDto responseDto = 
            environmentService.updateEnvironmentName(authenticatedEmail, envId, request);
        
        return ResponseEntity.ok(ApiResponse.success(
            responseDto, 
            "가상환경 이름이 수정되었습니다."
        ));
    }

    /**
     * 6. 🚨 (새로 추가) [Delete] 가상환경 삭제
     */
    @DeleteMapping("/{envId}")
    public ResponseEntity<ApiResponse<Void>> deleteEnvironment(
            @AuthenticationPrincipal String authenticatedEmail,
            @PathVariable("envId") Long envId
    ) {
        environmentService.deleteEnvironment(authenticatedEmail, envId);
        
        return ResponseEntity.ok(ApiResponse.success("가상환경이 삭제되었습니다."));
    }
}