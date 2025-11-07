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

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // 1. Long 타입 ID, 자동 증가 PK로 설정
    private Long id;

    @Column(unique = true, nullable = false) // 2. email은 PK가 아니지만, 고유해야 함
    private String email; 

    private String name;

    private String phoneNumber;
    
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