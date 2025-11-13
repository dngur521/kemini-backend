package com.opensource.kemini_backend.controller;

import com.opensource.kemini_backend.dto.ApiResponse;
import com.opensource.kemini_backend.service.S3Service;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/files")
public class FileController {

    private final S3Service s3Service;

    public FileController(S3Service s3Service) {
        this.s3Service = s3Service;
    }

    /**
     * 테스트용 파일 업로드 API
     * (이 API는 자동으로 인증이 필요합니다 - SecurityConfig)
     * @param file "file"이라는 키로 전송된 파일 데이터
     * @return
     */
    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadFile(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal String authenticatedEmail // (인증 확인용)
    ) {
        // (디버깅) 인증된 사용자 이메일 출력
        System.out.println("File upload request from: " + authenticatedEmail);
        
        try {
            // 1. S3 서비스 호출
            String fileUrl = s3Service.uploadFile(file);
            
            // 2. 성공 응답 반환
            return ResponseEntity.ok(ApiResponse.success(
                    Map.of("fileUrl", fileUrl),
                    "파일 업로드에 성공했습니다."
            ));

        } catch (IOException e) {
            throw new RuntimeException("파일 업로드 중 오류 발생: " + e.getMessage());
        }
    }
}