package com.docstorer.dto.response;

import com.docstorer.entity.ActivityLog;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data @Builder
public class ActivityLogResponse {
    private Long id;
    private String username;
    private ActivityLog.Action action;
    private String entityType;
    private Long entityId;
    private String entityName;
    private String description;
    private String ipAddress;
    private Boolean isSuccess;
    private LocalDateTime createdAt;

    public static ActivityLogResponse from(ActivityLog log) {
        return ActivityLogResponse.builder()
                .id(log.getId())
                .username(log.getUser() != null ? log.getUser().getUsername() : "System")
                .action(log.getAction())
                .entityType(log.getEntityType())
                .entityId(log.getEntityId())
                .entityName(log.getEntityName())
                .description(log.getDescription())
                .ipAddress(log.getIpAddress())
                .isSuccess(log.getIsSuccess())
                .createdAt(log.getCreatedAt())
                .build();
    }
}
