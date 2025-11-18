package com.opensource.kemini_backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "virtual_environments")
@Getter
@Setter
@NoArgsConstructor
public class VirtualEnvironment {

    // virtualEnvId PK
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; 

    // 가상환경 이름
    @Column(nullable = false)
    private String name; 

    // S3 버킷의 경로 (e.g, users/1/123/scene.dat)
    @Column(name = "s3_object_key", unique = true)
    private String s3ObjectKey; 

    // user_id: 회원 id (FK)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user; 

    public VirtualEnvironment(User user, String name) {
        this.user = user;
        this.name = name;
    }
}