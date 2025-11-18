package com.opensource.kemini_backend.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "users")
public class User {

    // ID: Long 타입, 자동 증가 PK로 설정
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) 
    private Long id;

    // email: PK가 아니지만, 고유해야 함
    @Column(unique = true, nullable = false) 
    private String email; 

    // 이름
    private String name;

    // 전화번호
    private String phoneNumber;

    // 보안 질문 ID
    @Column(name = "ask_id")
    private Long askId;

    // 보안 질문 답변
    @Column(name = "ask_answer")
    private String askAnswer;
    
    // Cognito 상태 추적용: UNCONFIRMED, CONFIRMED
    private String status; 

    // 정보 수정을 위한 메서드
    public void updateDetails(String name, String phoneNumber) {
        if (name != null) {
            this.name = name;
        }
        if (phoneNumber != null) {
            this.phoneNumber = phoneNumber;
        }
    }
}