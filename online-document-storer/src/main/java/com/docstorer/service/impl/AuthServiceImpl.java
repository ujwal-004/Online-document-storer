package com.docstorer.service.impl;

import com.docstorer.dto.request.LoginRequest;
import com.docstorer.dto.request.RegisterRequest;
import com.docstorer.dto.response.AuthResponse;
import com.docstorer.dto.response.UserResponse;
import com.docstorer.entity.ActivityLog;
import com.docstorer.entity.User;
import com.docstorer.exception.ResourceNotFoundException;
import com.docstorer.repository.ActivityLogRepository;
import com.docstorer.repository.UserRepository;
import com.docstorer.security.JwtTokenProvider;
import com.docstorer.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;
    private final ActivityLogRepository activityLogRepository;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail()))
            throw new IllegalArgumentException("Email already registered: " + request.getEmail());
        if (userRepository.existsByUsername(request.getUsername()))
            throw new IllegalArgumentException("Username already taken: " + request.getUsername());

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .role(User.Role.USER)
                .isActive(true)
                .isVerified(false)
                .build();
        userRepository.save(user);

        logActivity(user, ActivityLog.Action.REGISTER, "User", user.getId(), user.getUsername(),
                "New user registered", null, null, true);

        UserDetails userDetails = org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail())
                .password(user.getPassword())
                .roles(user.getRole().name())
                .build();

        String accessToken = jwtTokenProvider.generateToken(userDetails);
        String refreshToken = jwtTokenProvider.generateRefreshToken(userDetails);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getExpirationTime())
                .user(UserResponse.from(user))
                .build();
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request, String ipAddress, String userAgent) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmailOrUsername(), request.getPassword()));

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        User user = userRepository.findByEmailOrUsername(userDetails.getUsername())
                .or(() -> userRepository.findByEmail(userDetails.getUsername()))
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);

        logActivity(user, ActivityLog.Action.LOGIN, "User", user.getId(), user.getUsername(),
                "User logged in", ipAddress, userAgent, true);

        String accessToken = jwtTokenProvider.generateToken(userDetails);
        String refreshToken = jwtTokenProvider.generateRefreshToken(userDetails);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getExpirationTime())
                .user(UserResponse.from(user))
                .build();
    }

    @Override
    public AuthResponse refreshToken(String refreshToken) {
        String username = jwtTokenProvider.extractUsername(refreshToken);
        User user = userRepository.findByEmailOrUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        UserDetails userDetails = org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail())
                .password(user.getPassword())
                .roles(user.getRole().name())
                .build();

        if (!jwtTokenProvider.isTokenValid(refreshToken, userDetails))
            throw new IllegalArgumentException("Invalid or expired refresh token");

        String newAccessToken = jwtTokenProvider.generateToken(userDetails);
        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getExpirationTime())
                .user(UserResponse.from(user))
                .build();
    }

    @Override
    public void logout(String token) {
        log.info("User logged out");
    }

    private void logActivity(User user, ActivityLog.Action action, String entityType, Long entityId,
                             String entityName, String description, String ip, String ua, boolean success) {
        ActivityLog log = ActivityLog.builder()
                .user(user).action(action).entityType(entityType).entityId(entityId)
                .entityName(entityName).description(description)
                .ipAddress(ip).userAgent(ua).isSuccess(success).build();
        activityLogRepository.save(log);
    }
}
