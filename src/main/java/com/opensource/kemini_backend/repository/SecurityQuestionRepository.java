package com.opensource.kemini_backend.repository;

import com.opensource.kemini_backend.model.SecurityQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

/**
 * SecurityQuestion 엔티티를 위한 JpaRepository
 */
public interface SecurityQuestionRepository extends JpaRepository<SecurityQuestion, Long> {
    List<SecurityQuestion> findAllByOrderByIdAsc();
}