package com.docstorer.controller;

import com.docstorer.dto.response.*;
import com.docstorer.entity.User;
import com.docstorer.exception.ResourceNotFoundException;
import com.docstorer.repository.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin", description = "Admin-only endpoints")
public class AdminController {

    private final UserRepository userRepository;
    private final DocumentRepository documentRepository;
    private final ActivityLogRepository activityLogRepository;

    @GetMapping("/users")
    @Operation(summary = "Get all users (paginated)")
    public ResponseEntity<ApiResponse<Page<UserResponse>>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<UserResponse> users = userRepository.findAll(pageable).map(UserResponse::from);
        return ResponseEntity.ok(ApiResponse.success(users));
    }

    @GetMapping("/users/{id}")
    @Operation(summary = "Get user by ID")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        return ResponseEntity.ok(ApiResponse.success(UserResponse.from(user)));
    }

    @PutMapping("/users/{id}/toggle-active")
    @Operation(summary = "Enable or disable a user account")
    public ResponseEntity<ApiResponse<UserResponse>> toggleActive(@PathVariable Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        user.setIsActive(!user.getIsActive());
        userRepository.save(user);
        String msg = user.getIsActive() ? "User activated" : "User deactivated";
        return ResponseEntity.ok(ApiResponse.success(msg, UserResponse.from(user)));
    }

    @PutMapping("/users/{id}/role")
    @Operation(summary = "Change user role")
    public ResponseEntity<ApiResponse<UserResponse>> changeRole(
            @PathVariable Long id, @RequestParam String role) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        user.setRole(User.Role.valueOf(role.toUpperCase()));
        userRepository.save(user);
        return ResponseEntity.ok(ApiResponse.success("Role updated", UserResponse.from(user)));
    }

    @GetMapping("/documents")
    @Operation(summary = "Get all documents in the system")
    public ResponseEntity<ApiResponse<Page<DocumentResponse>>> getAllDocuments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<DocumentResponse> docs = documentRepository.findAll(pageable).map(d ->
                DocumentResponse.builder()
                        .id(d.getId()).originalName(d.getOriginalName()).fileType(d.getFileType())
                        .fileSize(d.getFileSize())
                        .fileSizeFormatted(DocumentResponse.formatFileSize(d.getFileSize() != null ? d.getFileSize() : 0))
                        .status(d.getStatus()).owner(UserResponse.from(d.getOwner()))
                        .createdAt(d.getCreatedAt()).build());
        return ResponseEntity.ok(ApiResponse.success(docs));
    }

    @GetMapping("/activities")
    @Operation(summary = "Get all system activity logs")
    public ResponseEntity<ApiResponse<Page<ActivityLogResponse>>> getAllActivities(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(ApiResponse.success(
                activityLogRepository.findAllByOrderByCreatedAtDesc(pageable)
                        .map(ActivityLogResponse::from)));
    }

    @GetMapping("/stats")
    @Operation(summary = "Get system-wide stats")
    public ResponseEntity<ApiResponse<DashboardStatsResponse>> getStats() {
        long totalUsers = userRepository.count();
        long totalDocs = documentRepository.countActiveDocs();
        long totalStorage = userRepository.findAll().stream()
                .mapToLong(u -> u.getStorageUsed() != null ? u.getStorageUsed() : 0L).sum();
        DashboardStatsResponse stats = DashboardStatsResponse.builder()
                .totalUsers(totalUsers)
                .totalSystemDocuments(totalDocs)
                .totalSystemStorage(totalStorage)
                .build();
        return ResponseEntity.ok(ApiResponse.success(stats));
    }
}
