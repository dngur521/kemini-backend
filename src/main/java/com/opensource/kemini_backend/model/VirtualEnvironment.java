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

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 1. "virtualEnvId"로 사용될 PK

    @Column(nullable = false)
    private String name; // (예: "My First World")

    @Column(name = "s3_object_key", unique = true)
    private String s3ObjectKey; // 2. "s3 버킷의 경로" (예: users/1/123/scene.dat)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user; // 3. "user_id" (FK)

    public VirtualEnvironment(User user, String name) {
        this.user = user;
        this.name = name;
    }
}