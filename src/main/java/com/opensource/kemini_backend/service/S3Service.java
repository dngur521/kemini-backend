package com.opensource.kemini_backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.UUID;

@Service
public class S3Service {

    private final S3Client s3Client;
    private final String bucketName;
    private final String s3Region;

    // 1. (의존성 추가 1단계 완료 시) S3Client는 자동 주입됩니다.
    public S3Service(S3Client s3Client,
                     @Value("${aws.s3.bucket}") String bucketName,
                     @Value("${spring.cloud.aws.region.static}") String s3Region) {
        this.s3Client = s3Client;
        this.bucketName = bucketName;
        this.s3Region = s3Region;
    }

    /**
     * 파일을 S3에 직접 업로드하고, 영구 URL을 반환합니다.
     * @param file 클라이언트(Unity)로부터 받은 파일
     * @return S3에 저장된 파일의 전체 URL
     * @throws IOException
     */
    public String uploadFile(MultipartFile file) throws IOException {
        
        // 1. 파일 확장자 추출 (예: .jpg)
        String extension = "";
        String originalFileName = file.getOriginalFilename();
        if (originalFileName != null && originalFileName.contains(".")) {
            extension = originalFileName.substring(originalFileName.lastIndexOf("."));
        }

        // 2. S3에 저장될 고유 키(경로) 생성
        // 예: uploads/test/랜덤UUID.jpg
        // (DB 저장을 안 하므로, 'test' 폴더에 모두 저장)
        String objectKey = String.format("uploads/test/%s%s",
                UUID.randomUUID(),
                extension
        );

        // 3. S3 업로드 요청 객체 생성
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .contentType(file.getContentType()) // (예: "image/jpeg")
                .contentLength(file.getSize())
                .build();

        // 4. S3로 파일 전송 (핵심 로직)
        s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(
            file.getInputStream(), 
            file.getSize()
        ));

        // 5. 저장된 파일의 영구 URL 반환
        // 예: https://kemini-bucket-이름.s3.ap-northeast-2.amazonaws.com/uploads/test/UUID.jpg
        return String.format("https://%s.s3.%s.amazonaws.com/%s",
                bucketName,
                s3Region,
                objectKey
        );
    }
}