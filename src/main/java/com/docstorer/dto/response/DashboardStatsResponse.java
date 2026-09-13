package com.docstorer.dto.response;

import lombok.Builder;
import lombok.Data;
import java.util.List;
import java.util.Map;

@Data @Builder
public class DashboardStatsResponse {
    private long totalDocuments;
    private long totalStorageUsed;
    private long storageLimit;
    private double storageUsedPercent;
    private String storageUsedFormatted;
    private long totalFolders;
    private long totalShared;
    private Map<String, Long> documentsByType;
    private List<DocumentResponse> recentDocuments;
    private List<ActivityLogResponse> recentActivities;
    // Admin-only
    private Long totalUsers;
    private Long totalSystemDocuments;
    private Long totalSystemStorage;
}
