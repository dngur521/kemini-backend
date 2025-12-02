package com.opensource.kemini_backend.service;

import com.opensource.kemini_backend.dto.*;
import com.opensource.kemini_backend.model.EnvironmentFile;
import com.opensource.kemini_backend.model.User;
import com.opensource.kemini_backend.model.VirtualEnvironment;
import com.opensource.kemini_backend.repository.UserRepository;
import com.opensource.kemini_backend.repository.VirtualEnvironmentRepository;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional // 서비스 전체에 트랜잭션 적용
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
     * [API 1] 새 가상환경 생성 (DB에만)
     */
    public VirtualEnvironmentResponseDto createEnvironment(String email, VirtualEnvironmentRequestDto request) {
        User user = findUserByEmail(email);

        VirtualEnvironment newEnv = new VirtualEnvironment(user, request.name());
        VirtualEnvironment savedEnv = envRepository.save(newEnv);

        // 생성 직후엔 파일 리스트가 비어있음
        return mapToDto(savedEnv);
    }

    /**
     * [API 2] 파일 업로드 URL 요청 및 파일 정보 DB 저장
     */
    public S3PresignedUrlResponseDto generateUploadUrl(String email, Long envId, S3PresignedUrlRequestDto request) {
        User user = findUserByEmail(email);
        VirtualEnvironment env = findEnvAndVerifyOwnership(envId, user.getId());

        // 1. S3 경로 생성: users/{uid}/{envId}/{TYPE}/{fileName}
        String objectKey = String.format("users/%d/%d/%s/%s",
                user.getId(),
                envId,
                request.fileType(), // SPACE or MARKER
                request.fileName()
        );

        // 2. Presigned URL 생성
        String presignedUrl = s3Service.generatePresignedUploadUrl(objectKey);
        String finalUrl = s3Service.getPublicFileUrl(objectKey);
        
        // 3. DB에 새 파일 정보 저장 (EnvironmentFile 추가)
        EnvironmentFile newFile = new EnvironmentFile(
            env, 
            request.fileType(), 
            request.fileName(), 
            objectKey
        );
        
        // Cascade 옵션 덕분에 부모 엔티티(env)를 저장하면 자식(newFile)도 자동 저장됨
        env.getFiles().add(newFile); 
        envRepository.save(env); 

        return new S3PresignedUrlResponseDto(presignedUrl, finalUrl);
    }
    
    /**
     * [GET] 특정 사용자의 모든 가상환경 조회
     */
    @Transactional(readOnly = true)
    public List<VirtualEnvironmentResponseDto> getAllEnvironments(String email) {
        User user = findUserByEmail(email);
        
        List<VirtualEnvironment> envs = envRepository.findByUser_IdOrderByIdDesc(user.getId());

        return envs.stream()
            .map(this::mapToDto)
            .collect(Collectors.toList());
    }

    /**
     * [GET] 단일 가상환경 상세 조회
     */
    @Transactional(readOnly = true)
    public VirtualEnvironmentResponseDto getEnvironmentById(String email, Long envId) {
        User user = findUserByEmail(email);
        VirtualEnvironment env = findEnvAndVerifyOwnership(envId, user.getId());
        
        return mapToDto(env);
    }

    /**
     * [PUT] 가상환경 이름 수정
     */
    public VirtualEnvironmentResponseDto updateEnvironmentName(String email, Long envId, VirtualEnvironmentRequestDto request) {
        User user = findUserByEmail(email);
        VirtualEnvironment env = findEnvAndVerifyOwnership(envId, user.getId());

        env.setName(request.name());
        VirtualEnvironment updatedEnv = envRepository.save(env);

        return mapToDto(updatedEnv);
    }

    /**
     * [DELETE] 가상환경 삭제 (포함된 모든 파일 삭제)
     */
    public void deleteEnvironment(String email, Long envId) {
        User user = findUserByEmail(email);
        VirtualEnvironment env = findEnvAndVerifyOwnership(envId, user.getId());

        // 1. 🚨 S3에서 연결된 모든 파일 삭제
        // (VirtualEnvironment -> EnvironmentFile 리스트 순회)
        for (EnvironmentFile file : env.getFiles()) {
            s3Service.deleteFile(file.getS3ObjectKey());
        }

        // 2. DB에서 가상환경 삭제 (Cascade로 파일 레코드들도 자동 삭제됨)
        envRepository.delete(env);
    }


    // --- 헬퍼 메서드 ---

    // 🚨 (수정됨) DTO 변환 헬퍼: 파일 리스트를 포함하도록 변경
    private VirtualEnvironmentResponseDto mapToDto(VirtualEnvironment env) {
        
        // EnvironmentFile 엔티티 리스트 -> EnvironmentFileDto 리스트 변환
        List<EnvironmentFileDto> fileDtos = env.getFiles().stream()
            .map(file -> EnvironmentFileDto.from(
                file, 
                s3Service.getPublicFileUrl(file.getS3ObjectKey())
            ))
            .collect(Collectors.toList());

        return new VirtualEnvironmentResponseDto(
            env.getId(), 
            env.getName(), 
            env.getUser().getId(),
            fileDtos // 파일 리스트 전달
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