package com.docstorer.dto.response;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data @Builder
public class FolderResponse {
    private Long id;
    private String name;
    private String description;
    private String color;
    private Long parentId;
    private String parentName;
    private int documentCount;
    private LocalDateTime createdAt;
}
