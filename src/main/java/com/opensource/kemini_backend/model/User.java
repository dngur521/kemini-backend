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
    // Cognito ID(sub)를 Primary Key로 사용하거나, 여기서는 email을 PK로 사용
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

    // 이메일 외의 다른 컬럼들은 필요에 따라 추가
}
