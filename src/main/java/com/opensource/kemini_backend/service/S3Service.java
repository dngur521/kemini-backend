package com.opensource.kemini_backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.net.URL;
import java.time.Duration;

@Service
public class S3Service {

    private final S3Presigner s3Presigner;
    private final S3Client s3Client;
    private final String bucketName;
    private final String s3Region;

    public S3Service(S3Presigner s3Presigner,
                     S3Client s3Client,
                     @Value("${aws.s3.bucket}") String bucketName,
                     @Value("${spring.cloud.aws.region.static}") String s3Region) {
        this.s3Presigner = s3Presigner;
        this.s3Client = s3Client; // 5. 🚨 할당
        this.bucketName = bucketName;
        this.s3Region = s3Region;
    }

    /**
     * S3에 업로드할 1회용 Presigned URL을 생성합니다. (PUT 방식)
     * @param objectKey S3에 저장될 전체 경로 (예: users/1/123/scene.dat)
     * @return 1회용 업로드 URL
     */
    public String generatePresignedUploadUrl(String objectKey) {
        try {
            PutObjectRequest objectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectKey)
                    .build();

            PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofMinutes(10)) // 10분 유효
                    .putObjectRequest(objectRequest)
                    .build();

            URL url = s3Presigner.presignPutObject(presignRequest).url();
            return url.toString();

        } catch (Exception e) {
            throw new RuntimeException("Presigned URL 생성 실패: " + e.getMessage());
        }
    }

    /**
     * S3 키(경로)를 기반으로 파일에 접근할 수 있는 영구 URL을 생성합니다.
     */
    public String getPublicFileUrl(String objectKey) {
        return String.format("https://%s.s3.%s.amazonaws.com/%s",
                bucketName,
                s3Region,
                objectKey
        );
    }
    /**
     * 6. 🚨 (새로 추가) S3 객체 삭제 메서드
     */
    public void deleteFile(String objectKey) {
        if (objectKey == null || objectKey.isBlank()) {
            return; // 삭제할 키가 없으면 무시
        }
        
        try {
            DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectKey)
                    .build();
            
            s3Client.deleteObject(deleteRequest);
            
        } catch (Exception e) {
            // S3에서 파일 삭제 실패 시, 일단 로그만 남기고 DB 삭제는 진행되도록 함
            // (운영 정책에 따라 이 부분에서 예외를 던져 DB 롤백을 유도할 수도 있음)
            System.err.println("S3 파일 삭제 실패: " + objectKey + ", Error: " + e.getMessage());
        }
    }
}