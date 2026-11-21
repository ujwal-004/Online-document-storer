package com.docstorer.config;

import com.docstorer.entity.User;
import com.docstorer.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class DataInitializer {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.file.upload-dir}")
    private String uploadDir;

    @Value("${app.admin.email:}")
    private String adminEmail;

    @Value("${app.admin.password:}")
    private String adminPassword;

    @Value("${app.admin.name}")
    private String adminName;

    @Bean
    public CommandLineRunner init() {
        return args -> {
            // Create upload directory
            try {
                Path uploadPath = Paths.get(uploadDir);
                if (!Files.exists(uploadPath)) {
                    Files.createDirectories(uploadPath);
                    log.info("Created upload directory: {}", uploadPath.toAbsolutePath());
                }
                // Create subdirectories
                for (String subDir : new String[]{"documents", "avatars", "temp", "versions"}) {
                    Path sub = uploadPath.resolve(subDir);
                    if (!Files.exists(sub)) Files.createDirectories(sub);
                }
            } catch (IOException e) {
                log.error("Could not create upload directory: {}", e.getMessage());
            }

            // Seed an initial administrator only when both values are configured.
            if (adminEmail == null || adminEmail.isBlank()
                    || adminPassword == null || adminPassword.isBlank()) {
                log.warn("No admin account was seeded. Set ADMIN_EMAIL and ADMIN_PASSWORD before first deployment.");
                return;
            }
            if (adminPassword.length() < 12
                    || adminPassword.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72) {
                throw new IllegalStateException("ADMIN_PASSWORD must be 12–72 UTF-8 bytes.");
            }

            String normalizedEmail = adminEmail.trim().toLowerCase(java.util.Locale.ROOT);
            if (!userRepository.existsByEmail(normalizedEmail)) {
                User admin = User.builder()
                        .username("admin")
                        .email(normalizedEmail)
                        .password(passwordEncoder.encode(adminPassword))
                        .firstName(adminName == null || adminName.isBlank() ? "System" : adminName)
                        .lastName("Administrator")
                        .role(User.Role.ADMIN)
                        .isActive(true)
                        .isVerified(true)
                        .storageLimit(Long.MAX_VALUE)
                        .build();
                userRepository.save(admin);
                log.info("Initial administrator account created from environment settings: {}", normalizedEmail);
            }
        };
    }
}
