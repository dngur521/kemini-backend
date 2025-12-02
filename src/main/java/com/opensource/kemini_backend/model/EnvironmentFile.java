package com.opensource.kemini_backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "environment_files")
@Getter
@Setter
@NoArgsConstructor
public class EnvironmentFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 파일의 종류 (SPACE: 공간 데이터, MARKER: 마커/가구 데이터)
    @Column(nullable = false)
    private String fileType; 

    @Column(nullable = false)
    private String originalFileName;

    @Column(nullable = false)
    private String s3ObjectKey;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "virtual_environment_id")
    private VirtualEnvironment virtualEnvironment;

    public EnvironmentFile(VirtualEnvironment env, String fileType, String originalFileName, String s3ObjectKey) {
        this.virtualEnvironment = env;
        this.fileType = fileType;
        this.originalFileName = originalFileName;
        this.s3ObjectKey = s3ObjectKey;
    }
}