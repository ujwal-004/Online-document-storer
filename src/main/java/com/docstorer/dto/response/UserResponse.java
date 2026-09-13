package com.docstorer.dto.response;

import com.docstorer.entity.User;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data @Builder
public class UserResponse {
    private Long id;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private String fullName;
    private String profilePicture;
    private String phoneNumber;
    private User.Role role;
    private Boolean isActive;
    private Boolean darkMode;
    private Long storageUsed;
    private Long storageLimit;
    private LocalDateTime lastLogin;
    private LocalDateTime createdAt;

    public static UserResponse from(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .fullName(user.getFullName())
                .profilePicture(user.getProfilePicture())
                .phoneNumber(user.getPhoneNumber())
                .role(user.getRole())
                .isActive(user.getIsActive())
                .darkMode(user.getDarkMode())
                .storageUsed(user.getStorageUsed())
                .storageLimit(user.getStorageLimit())
                .lastLogin(user.getLastLogin())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
