package com.opensource.kemini_backend.controller;

import com.opensource.kemini_backend.service.AIService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/ai")
public class AIController {

    private final AIService aiService;

    public AIController(AIService aiService) {
        this.aiService = aiService;
    }


    // Unity 클라이언트로부터 이미지를 받아 AI 서버로 중계(proxy)하는 API
    @PostMapping("/generate-model")
    public ResponseEntity<byte[]> generateModel(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal String authenticatedEmail
    ) {
        
        System.out.println("AI 모델 생성 요청 (사용자: " + authenticatedEmail + ")");

        // 1. AIService가 (API 키 없이) AI 서버와 통신하고 .glb(byte[])를 가져옴
        byte[] aiResponseBytes = aiService.generate3DModel(file);

        // 2. Unity에게 이게 .glb 파일이라고 알려주는 헤더 생성
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.valueOf("model/gltf-binary"));
        headers.setContentDispositionFormData("attachment", "generated_model.glb");

        // 3. Unity로 .glb 파일(byte[])을 직접 반환
        return ResponseEntity.ok()
                .headers(headers)
                .body(aiResponseBytes);
    }
}