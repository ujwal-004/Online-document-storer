package com.docstorer.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "activity_logs", indexes = {
    @Index(name = "idx_log_user", columnList = "user_id"),
    @Index(name = "idx_log_action", columnList = "action"),
    @Index(name = "idx_log_created", columnList = "created_at")
})
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
@Builder
public class ActivityLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private Action action;

    @Column(name = "entity_type", length = 50)
    private String entityType;

    @Column(name = "entity_id")
    private Long entityId;

    @Column(name = "entity_name", length = 255)
    private String entityName;

    @Column(length = 500)
    private String description;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "is_success")
    @Builder.Default
    private Boolean isSuccess = true;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public enum Action {
        LOGIN, LOGOUT, REGISTER,
        UPLOAD_DOCUMENT, DOWNLOAD_DOCUMENT, DELETE_DOCUMENT, VIEW_DOCUMENT,
        UPDATE_DOCUMENT, SHARE_DOCUMENT, RESTORE_DOCUMENT,
        CREATE_FOLDER, DELETE_FOLDER, RENAME_FOLDER,
        UPDATE_PROFILE, CHANGE_PASSWORD,
        SEARCH, ADMIN_ACTION
    }
}
