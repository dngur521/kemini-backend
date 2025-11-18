package com.opensource.kemini_backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

@Service
public class AIService {

    private final RestTemplate restTemplate;
    private final String aiServerUrl;

    // 생성자
    public AIService(RestTemplate restTemplate,
                     @Value("${ai.server.url}") String aiServerUrl) {
        this.restTemplate = restTemplate;
        this.aiServerUrl = aiServerUrl;
    }

    /**
     * 이미지를 AI 서버로 중계하고, AI가 생성한 .glb(바이너리)를 byte[]로 반환합니다.
     */
    public byte[] generate3DModel(MultipartFile imageFile) {
        try {
            // 헤더 생성
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            // AI 서버로 보낼 'file' 폼 데이터 생성
            ByteArrayResource fileAsResource = new ByteArrayResource(imageFile.getBytes()) {
                @Override
                public String getFilename() {
                    return imageFile.getOriginalFilename();
                }
            };
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", fileAsResource);

            // 헤더와 바디를 합쳐 HTTP 요청 엔티티 생성
            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            // AI 서버에 POST 요청 (byte[].class로 응답 받음)
            ResponseEntity<byte[]> response = restTemplate.postForEntity(
                aiServerUrl,
                requestEntity,
                byte[].class
            );

            // AI 서버가 보낸 3D 모델(바이너리)을 그대로 반환
            return response.getBody();

        } catch (Exception e) {
            throw new RuntimeException("AI 서버 통신 오류: " + e.getMessage());
        }
    }
}