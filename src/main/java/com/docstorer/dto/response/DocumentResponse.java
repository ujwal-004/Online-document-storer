package com.docstorer.dto.response;

import com.docstorer.entity.Document;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data @Builder
public class DocumentResponse {
    private Long id;
    private String fileName;
    private String originalName;
    private String fileType;
    private String mimeType;
    private Long fileSize;
    private String fileSizeFormatted;
    private String description;
    private Document.DocumentStatus status;
    private Boolean isEncrypted;
    private Boolean isShared;
    private String shareToken;
    private LocalDateTime shareExpiry;
    private Integer downloadCount;
    private Integer viewCount;
    private Integer version;
    private String category;
    private List<String> tags;
    private Long folderId;
    private String folderName;
    private UserResponse owner;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
    }
}
