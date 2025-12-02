package com.opensource.kemini_backend.dto;

import com.opensource.kemini_backend.model.EnvironmentFile;

public record EnvironmentFileDto(
    Long fileId,
    String fileType,
    String fileName,
    String fileUrl
) {
    // Service에서 URL 생성 후 주입받음
    public static EnvironmentFileDto from(EnvironmentFile file, String url) {
        return new EnvironmentFileDto(
            file.getId(),
            file.getFileType(),
            file.getOriginalFileName(),
            url
        );
    }
}