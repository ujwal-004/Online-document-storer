package com.docstorer.service.impl;

import com.docstorer.dto.request.UpdateProfileRequest;
import com.docstorer.dto.response.*;
import com.docstorer.entity.*;
import com.docstorer.exception.ResourceNotFoundException;
import com.docstorer.repository.*;
import com.docstorer.service.UserService;
import com.docstorer.util.FileStorageUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final DocumentRepository documentRepository;
    private final FolderRepository folderRepository;
    private final ActivityLogRepository activityLogRepository;
    private final FileStorageUtil fileStorageUtil;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UserResponse getProfile(String email) {
        return UserResponse.from(getUser(email));
    }

    @Override
    @Transactional
    public UserResponse updateProfile(String email, UpdateProfileRequest req) {
        User user = getUser(email);
        if (req.getFirstName() != null) user.setFirstName(req.getFirstName());
        if (req.getLastName() != null) user.setLastName(req.getLastName());
        if (req.getPhoneNumber() != null) user.setPhoneNumber(req.getPhoneNumber());
        if (req.getDarkMode() != null) user.setDarkMode(req.getDarkMode());
        return UserResponse.from(userRepository.save(user));
    }

    @Override
    @Transactional
    public UserResponse uploadAvatar(String email, MultipartFile file) {
        User user = getUser(email);
        String path = fileStorageUtil.storeFile(file, "avatars");
        user.setProfilePicture("/uploads/" + path);
        return UserResponse.from(userRepository.save(user));
    }

    @Override
    @Transactional
    public void changePassword(String email, String oldPassword, String newPassword) {
        User user = getUser(email);
        if (!passwordEncoder.matches(oldPassword, user.getPassword()))
            throw new IllegalArgumentException("Current password is incorrect");
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public DashboardStatsResponse getDashboardStats(String email) {
        User user = getUser(email);
        long totalDocs = documentRepository.findByOwnerAndStatusNot(
                user, Document.DocumentStatus.DELETED, Pageable.unpaged()).getTotalElements();
        Long storageUsed = documentRepository.sumFileSizeByOwner(user);
        if (storageUsed == null) storageUsed = 0L;

        long folderCount = folderRepository.findByOwnerAndParentIsNull(user).size();
        long shared = user.getDocuments().stream().filter(Document::getIsShared).count();

        // Documents by type
        Map<String, Long> byType = new HashMap<>();
        documentRepository.countByFileTypeForOwner(user)
                .forEach(row -> byType.put((String) row[0], (Long) row[1]));

        // Recent documents
        List<DocumentResponse> recent = documentRepository
                .findByOwnerAndStatusNot(user, Document.DocumentStatus.DELETED,
                        PageRequest.of(0, 5, Sort.by("createdAt").descending()))
                .map(d -> DocumentResponse.builder()
                        .id(d.getId()).originalName(d.getOriginalName())
                        .fileType(d.getFileType()).fileSize(d.getFileSize())
                        .fileSizeFormatted(DocumentResponse.formatFileSize(d.getFileSize() != null ? d.getFileSize() : 0))
                        .createdAt(d.getCreatedAt()).status(d.getStatus()).build())
                .getContent();

        // Recent activities
        List<ActivityLogResponse> activities = activityLogRepository
                .findTop10ByUserOrderByCreatedAtDesc(user).stream()
                .map(ActivityLogResponse::from).collect(Collectors.toList());

        double pct = user.getStorageLimit() > 0
                ? (double) storageUsed / user.getStorageLimit() * 100 : 0;

        // Admin extras
        Long totalUsers = null;
        Long totalSystemDocs = null;
        Long totalSystemStorage = null;
        if (user.getRole() == User.Role.ADMIN) {
            totalUsers = userRepository.count();
            totalSystemDocs = documentRepository.countActiveDocs();
            totalSystemStorage = userRepository.findAll().stream()
                    .mapToLong(u -> u.getStorageUsed() != null ? u.getStorageUsed() : 0L).sum();
        }

        return DashboardStatsResponse.builder()
                .totalDocuments(totalDocs)
                .totalStorageUsed(storageUsed)
                .storageLimit(user.getStorageLimit())
                .storageUsedPercent(Math.min(pct, 100))
                .storageUsedFormatted(DocumentResponse.formatFileSize(storageUsed))
                .totalFolders(folderCount)
                .totalShared(shared)
                .documentsByType(byType)
                .recentDocuments(recent)
                .recentActivities(activities)
                .totalUsers(totalUsers)
                .totalSystemDocuments(totalSystemDocs)
                .totalSystemStorage(totalSystemStorage)
                .build();
    }

    @Override
    public Page<ActivityLogResponse> getActivityLogs(String email, Pageable pageable) {
        User user = getUser(email);
        return activityLogRepository.findByUser(user, pageable).map(ActivityLogResponse::from);
    }

    private User getUser(String email) {
        return userRepository.findByEmailOrUsername(email)
                .or(() -> userRepository.findByEmail(email))
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
    }
}
