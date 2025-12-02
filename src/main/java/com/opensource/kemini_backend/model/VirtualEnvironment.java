package com.opensource.kemini_backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "virtual_environments")
@Getter
@Setter
@NoArgsConstructor
public class VirtualEnvironment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    // 🚨 (삭제됨) private String s3ObjectKey; 

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // 🚨 (새로 추가) 1:N 관계 설정
    @OneToMany(mappedBy = "virtualEnvironment", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EnvironmentFile> files = new ArrayList<>();

    public VirtualEnvironment(User user, String name) {
        this.user = user;
        this.name = name;
    }
}