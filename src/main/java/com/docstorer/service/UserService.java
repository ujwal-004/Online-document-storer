package com.docstorer.service;

import com.docstorer.dto.request.UpdateProfileRequest;
import com.docstorer.dto.response.ActivityLogResponse;
import com.docstorer.dto.response.DashboardStatsResponse;
import com.docstorer.dto.response.UserResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

public interface UserService {
    UserResponse getProfile(String email);
    UserResponse updateProfile(String email, UpdateProfileRequest request);
    UserResponse uploadAvatar(String email, MultipartFile file);
    void changePassword(String email, String oldPassword, String newPassword);
    DashboardStatsResponse getDashboardStats(String email);
    Page<ActivityLogResponse> getActivityLogs(String email, Pageable pageable);
}
