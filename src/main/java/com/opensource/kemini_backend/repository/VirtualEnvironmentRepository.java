package com.opensource.kemini_backend.repository;

import com.opensource.kemini_backend.model.VirtualEnvironment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface VirtualEnvironmentRepository extends JpaRepository<VirtualEnvironment, Long> {
    /**
     * 유저 ID로 환경 목록 조회 (ID 내림차순 정렬)
     */
    List<VirtualEnvironment> findByUser_IdOrderByIdDesc(Long userId);
}