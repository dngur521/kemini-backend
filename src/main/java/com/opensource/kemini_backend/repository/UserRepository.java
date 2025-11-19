package com.opensource.kemini_backend.repository;

import org.springframework.transaction.annotation.Transactional;
import com.opensource.kemini_backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    // 이메일로 사용자 삭제 메서드
    @Transactional // 삭제는 트랜잭션이 필요
    void deleteByEmail(String email);
    
    // 이메일로 사용자 찾기
    Optional<User> findByEmail(String email);

    // 전화번호로 사용자 조회
    Optional<User> findByPhoneNumber(String phoneNumber);

    // 아이디 찾기를 위한 3개 필드 조회
    Optional<User> findByPhoneNumberAndAskIdAndAskAnswer(
        String phoneNumber, 
        Long askId, 
        String askAnswer
    );
}
