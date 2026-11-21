package com.docstorer.util;

import com.docstorer.exception.FileStorageException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.*;

@Component
@Slf4j
public class FileStorageUtil {
    private static final byte[] MAGIC = new byte[]{'O', 'D', 'S', '1'};
    private static final int NONCE_BYTES = 12;
    private static final int TAG_BITS = 128;

    @Value("${app.file.upload-dir}")
    private String uploadDir;

    @Value("${app.file.allowed-types}")
    private String allowedTypes;

    @Value("${app.encryption.key:}")
    private String encryptionKey;

    @Value("${app.file.max-size:52428800}")
    private long maxFileSize;

    public String storeFile(MultipartFile file, String subDirectory) {
        validateFile(file);
        try {
            Path directory = resolveDirectory(subDirectory);
            String extension = FilenameUtils.getExtension(file.getOriginalFilename()).toLowerCase(Locale.ROOT);
            String storedName = UUID.randomUUID() + "." + extension;
            Path target = directory.resolve(storedName).normalize();
            ensureInsideRoot(target);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            return subDirectory + "/" + storedName;
        } catch (IOException exception) {
            throw new FileStorageException("Could not store file. Please try again.", exception);
        }
    }

    public String storeEncryptedFile(MultipartFile file, String subDirectory) {
        validateFile(file);
        requireEncryptionKey();
        try {
            Path directory = resolveDirectory(subDirectory);
            String extension = FilenameUtils.getExtension(file.getOriginalFilename()).toLowerCase(Locale.ROOT);
            String storedName = UUID.randomUUID() + "." + extension + ".enc";
            Path target = directory.resolve(storedName).normalize();
            ensureInsideRoot(target);
            Files.write(target, encrypt(file.getBytes()), StandardOpenOption.CREATE_NEW);
            return subDirectory + "/" + storedName;
        } catch (Exception exception) {
            throw new FileStorageException("Could not store encrypted file.", exception);
        }
    }

    public byte[] loadFile(String filePath) {
        try {
            Path path = resolveStoredPath(filePath);
            if (!Files.isRegularFile(path)) throw new FileStorageException("File not found.");
            return Files.readAllBytes(path);
        } catch (IOException exception) {
            throw new FileStorageException("Could not read file.", exception);
        }
    }

    public byte[] loadDecryptedFile(String filePath) {
        requireEncryptionKey();
        try {
            return decrypt(loadFile(filePath));
        } catch (Exception exception) {
            throw new FileStorageException("Could not decrypt file. Check the encryption key and file integrity.", exception);
        }
    }

    public void deleteFile(String filePath) {
        try {
            Files.deleteIfExists(resolveStoredPath(filePath));
        } catch (IOException exception) {
            throw new FileStorageException("Could not delete file safely.", exception);
        }
    }

    public String copyFileAsVersion(String originalPath, int versionNumber) {
        if (versionNumber < 1) throw new IllegalArgumentException("Version number must be positive.");
        try {
            Path source = resolveStoredPath(originalPath);
            if (!Files.isRegularFile(source)) throw new FileStorageException("Source file not found.");
            Path directory = resolveDirectory("versions");
            String extension = FilenameUtils.getExtension(originalPath);
            String suffix = extension.isBlank() ? "" : "." + extension;
            String storedName = UUID.randomUUID() + "_v" + versionNumber + suffix;
            Path target = directory.resolve(storedName).normalize();
            ensureInsideRoot(target);
            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
            return "versions/" + storedName;
        } catch (IOException exception) {
            throw new FileStorageException("Could not create version copy.", exception);
        }
    }

    public String computeChecksum(MultipartFile file) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(file.getBytes());
            StringBuilder result = new StringBuilder();
            for (byte value : hash) result.append(String.format("%02x", value));
            return result.toString();
        } catch (Exception exception) {
            log.warn("Could not compute uploaded-file checksum", exception);
            return null;
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new FileStorageException("File is empty.");
        if (file.getSize() > maxFileSize) throw new FileStorageException("File exceeds the configured upload limit.");
        String originalName = file.getOriginalFilename();
        if (originalName == null || originalName.isBlank()
                || originalName.contains("..") || originalName.contains("/")) {
            throw new FileStorageException("Invalid filename.");
        }
        String extension = FilenameUtils.getExtension(originalName).toLowerCase(Locale.ROOT);
        Set<String> allowed = new HashSet<>();
        for (String type : allowedTypes.split(",")) allowed.add(type.trim().toLowerCase(Locale.ROOT));
        if (!allowed.contains(extension)) throw new FileStorageException("File extension is not allowed.");
    }

    private Path uploadRoot() throws IOException {
        Path configured = Paths.get(uploadDir).toAbsolutePath().normalize();
        Files.createDirectories(configured);
        return configured.toRealPath();
    }

    private Path resolveDirectory(String subDirectory) throws IOException {
        if (subDirectory == null || !subDirectory.matches("[A-Za-z0-9_-]+")) {
            throw new FileStorageException("Invalid storage directory.");
        }
        Path root = uploadRoot();
        Path directory = root.resolve(subDirectory).normalize();
        if (!directory.startsWith(root)) throw new FileStorageException("Storage path is outside the upload directory.");
        Files.createDirectories(directory);
        Path realDirectory = directory.toRealPath();
        if (!realDirectory.startsWith(root)) throw new FileStorageException("Storage path resolves outside the upload directory.");
        return realDirectory;
    }

    private Path resolveStoredPath(String relativePath) throws IOException {
        if (relativePath == null || relativePath.isBlank()) throw new FileStorageException("Invalid stored path.");
        Path relative = Paths.get(relativePath);
        if (relative.isAbsolute()) throw new FileStorageException("Absolute file paths are not allowed.");

        Path root = uploadRoot();
        Path target = root.resolve(relative).normalize();
        if (!target.startsWith(root) || target.equals(root)) {
            throw new FileStorageException("Stored path is outside the upload directory.");
        }
        Path parent = target.getParent();
        if (parent == null) throw new FileStorageException("Invalid stored path.");
        Files.createDirectories(parent);
        Path realParent = parent.toRealPath();
        if (!realParent.startsWith(root)) throw new FileStorageException("Stored path resolves outside the upload directory.");
        Path safeTarget = realParent.resolve(target.getFileName());
        if (Files.exists(safeTarget)) {
            Path realTarget = safeTarget.toRealPath();
            if (!realTarget.startsWith(root)) throw new FileStorageException("Stored file resolves outside the upload directory.");
            return realTarget;
        }
        return safeTarget;
    }

    private void ensureInsideRoot(Path target) throws IOException {
        Path root = uploadRoot();
        if (!target.normalize().startsWith(root)) throw new FileStorageException("File path is outside the upload directory.");
        Path realParent = target.getParent().toRealPath();
        if (!realParent.startsWith(root)) throw new FileStorageException("File directory resolves outside the upload directory.");
    }

    private void requireEncryptionKey() {
        if (encryptionKey == null || encryptionKey.length() < 32) {
            throw new FileStorageException("File encryption requires ENCRYPTION_KEY to be at least 32 characters.");
        }
    }

    private byte[] encryptionKeyBytes() throws Exception {
        return MessageDigest.getInstance("SHA-256").digest(encryptionKey.getBytes(StandardCharsets.UTF_8));
    }

    private byte[] encrypt(byte[] data) throws Exception {
        byte[] nonce = new byte[NONCE_BYTES];
        new SecureRandom().nextBytes(nonce);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(encryptionKeyBytes(), "AES"),
                new GCMParameterSpec(TAG_BITS, nonce));
        byte[] encrypted = cipher.doFinal(data);
        return ByteBuffer.allocate(MAGIC.length + nonce.length + encrypted.length)
                .put(MAGIC).put(nonce).put(encrypted).array();
    }

    private byte[] decrypt(byte[] data) throws Exception {
        if (data.length >= MAGIC.length + NONCE_BYTES + 16
                && Arrays.equals(Arrays.copyOfRange(data, 0, MAGIC.length), MAGIC)) {
            byte[] nonce = Arrays.copyOfRange(data, MAGIC.length, MAGIC.length + NONCE_BYTES);
            byte[] cipherText = Arrays.copyOfRange(data, MAGIC.length + NONCE_BYTES, data.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(encryptionKeyBytes(), "AES"),
                    new GCMParameterSpec(TAG_BITS, nonce));
            return cipher.doFinal(cipherText);
        }

        // Backward-compatible reader for files produced by the previous AES/ECB storage format.
        byte[] legacyKey = Arrays.copyOf(encryptionKey.getBytes(StandardCharsets.UTF_8), 32);
        Cipher legacyCipher = Cipher.getInstance("AES");
        legacyCipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(legacyKey, "AES"));
        return legacyCipher.doFinal(data);
    }

    public String getUploadDir() {
        return uploadDir;
    }
}
