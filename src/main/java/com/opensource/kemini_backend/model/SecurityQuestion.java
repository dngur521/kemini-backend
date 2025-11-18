package com.opensource.kemini_backend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 보안 질문 리스트를 저장하는 엔티티
 */
@Entity
@Table(name = "security_questions")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SecurityQuestion {

    // 질문 ID (이것이 User 엔티티의 askId가 됩니다)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 질문 내용 (예: "졸업한 초등학교 이름은?")
    @Column(nullable = false, unique = true)
    private String questionText;
}