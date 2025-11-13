package com.opensource.kemini_backend.service;

import com.opensource.kemini_backend.dto.S3PresignedUrlRequestDto;
import com.opensource.kemini_backend.dto.S3PresignedUrlResponseDto;
import com.opensource.kemini_backend.dto.VirtualEnvironmentRequestDto; // (아래에서 생성)
import com.opensource.kemini_backend.dto.VirtualEnvironmentResponseDto; // (아래에서 생성)
import com.opensource.kemini_backend.model.User;
import com.opensource.kemini_backend.model.VirtualEnvironment;
import com.opensource.kemini_backend.repository.UserRepository;
import com.opensource.kemini_backend.repository.VirtualEnvironmentRepository;

import java.util.stream.Collectors;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EnvironmentService {

    private final VirtualEnvironmentRepository envRepository;
    private final UserRepository userRepository;
    private final S3Service s3Service;

    public EnvironmentService(VirtualEnvironmentRepository envRepository, UserRepository userRepository, S3Service s3Service) {
        this.envRepository = envRepository;
        this.userRepository = userRepository;
        this.s3Service = s3Service;
    }

    /**
     * API 1: 새 가상환경 생성 (DB에만)
     */
    public VirtualEnvironmentResponseDto createEnvironment(String email, VirtualEnvironmentRequestDto request) {
        User user = findUserByEmail(email);

        VirtualEnvironment newEnv = new VirtualEnvironment(user, request.name());
        VirtualEnvironment savedEnv = envRepository.save(newEnv);

        // 3. 🚨 DTO 생성 방식 변경 (s3FileUrl은 null)
        return new VirtualEnvironmentResponseDto(
            savedEnv.getId(), 
            savedEnv.getName(), 
            null, 
            savedEnv.getUser().getId()
        );
    }

    /**
     * API 2: 파일 업로드 URL 요청 및 S3 경로 DB에 저장
     */
    @Transactional
    public S3PresignedUrlResponseDto generateUploadUrl(String email, Long envId, S3PresignedUrlRequestDto request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
        
        VirtualEnvironment env = envRepository.findById(envId)
                .orElseThrow(() -> new RuntimeException("가상환경을 찾을 수 없습니다."));

        // 1. 소유권 확인
        if (!env.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("이 가상환경에 대한 권한이 없습니다.");
        }

        // 2. 🚨 S3 Object Key 생성 (요청하신 형식)
        String objectKey = String.format("users/%d/%d/%s",
                user.getId(), // 1
                envId,        // 123
                request.fileName() // scene.dat
        );

        // 3. S3 서비스 호출
        String presignedUrl = s3Service.generatePresignedUploadUrl(objectKey);
        String finalUrl = s3Service.getPublicFileUrl(objectKey);
        
        // 4. 🚨 S3 경로를 DB에 저장
        env.setS3ObjectKey(objectKey); // (전체 URL 대신 Key를 저장하는 것이 더 유연함)
        envRepository.save(env);

        // 5. 클라이언트에게 2개 URL 반환
        return new S3PresignedUrlResponseDto(presignedUrl, finalUrl);
    }
    /**
     * 4. 🚨 (새로 추가) [Load] - 특정 사용자의 모든 가상환경 조회
     */
    @Transactional(readOnly = true) // 읽기 전용
    public List<VirtualEnvironmentResponseDto> getAllEnvironments(String email) {
        User user = findUserByEmail(email);
        
        // 1. 유저 ID로 모든 환경을 찾음 (JPA 쿼리 메서드 필요 - 5단계)
        List<VirtualEnvironment> envs = envRepository.findByUser_IdOrderByIdDesc(user.getId());

        // 2. DTO 리스트로 변환
        return envs.stream()
            .map(this::mapToDto) // 3. DTO 변환 헬퍼 사용
            .collect(Collectors.toList());
    }

    /**
     * 5. 🚨 (새로 추가) [Load] - 단일 가상환경 상세 조회
     */
    @Transactional(readOnly = true)
    public VirtualEnvironmentResponseDto getEnvironmentById(String email, Long envId) {
        User user = findUserByEmail(email);
        VirtualEnvironment env = findEnvAndVerifyOwnership(envId, user.getId());
        
        return mapToDto(env); // DTO 변환 헬퍼 사용
    }

    /**
     * 6. 🚨 (새로 추가) [Update] - 가상환경 이름 수정
     */
    public VirtualEnvironmentResponseDto updateEnvironmentName(String email, Long envId, VirtualEnvironmentRequestDto request) {
        User user = findUserByEmail(email);
        VirtualEnvironment env = findEnvAndVerifyOwnership(envId, user.getId());

        env.setName(request.name());
        VirtualEnvironment updatedEnv = envRepository.save(env);

        return mapToDto(updatedEnv);
    }

    /**
     * 7. 🚨 (새로 추가) [Delete] - 가상환경 삭제
     */
    public void deleteEnvironment(String email, Long envId) {
        User user = findUserByEmail(email);
        VirtualEnvironment env = findEnvAndVerifyOwnership(envId, user.getId());

        // 1. 🚨 S3에서 파일 먼저 삭제
        s3Service.deleteFile(env.getS3ObjectKey());

        // 2. 🚨 DB에서 레코드 삭제
        envRepository.delete(env);
    }


    // --- 헬퍼 메서드 ---

    // (DTO 변환 헬퍼)
    private VirtualEnvironmentResponseDto mapToDto(VirtualEnvironment env) {
        String fileUrl = null;
        if (env.getS3ObjectKey() != null) {
            fileUrl = s3Service.getPublicFileUrl(env.getS3ObjectKey());
        }
        return new VirtualEnvironmentResponseDto(
            env.getId(), 
            env.getName(), 
            fileUrl, 
            env.getUser().getId()
        );
    }

    // (사용자 조회 헬퍼)
    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
    }

    // (소유권 검증 헬퍼)
    private VirtualEnvironment findEnvAndVerifyOwnership(Long envId, Long userId) {
        VirtualEnvironment env = envRepository.findById(envId)
                .orElseThrow(() -> new RuntimeException("가상환경을 찾을 수 없습니다."));

        if (!env.getUser().getId().equals(userId)) {
            throw new RuntimeException("이 가상환경에 대한 권한이 없습니다.");
        }
        return env;
    }
}