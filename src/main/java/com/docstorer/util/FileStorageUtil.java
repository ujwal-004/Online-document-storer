package com.docstorer.util;

import com.docstorer.exception.FileStorageException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;

@Component
@Slf4j
public class FileStorageUtil {

    @Value("${app.file.upload-dir}")
    private String uploadDir;

    @Value("${app.file.allowed-types}")
    private String allowedTypes;

    @Value("${app.encryption.key}")
    private String encryptionKey;

    public String storeFile(MultipartFile file, String subDirectory) {
        validateFile(file);
        try {
            Path targetDir = Paths.get(uploadDir).resolve(subDirectory).toAbsolutePath().normalize();
            Files.createDirectories(targetDir);
            String ext = FilenameUtils.getExtension(
                Objects.requireNonNull(file.getOriginalFilename())).toLowerCase();
            String storedName = UUID.randomUUID() + "." + ext;
            Path targetPath = targetDir.resolve(storedName);
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
            log.info("File stored: {}", targetPath);
            return subDirectory + "/" + storedName;
        } catch (IOException ex) {
            throw new FileStorageException("Could not store file. Please try again.", ex);
        }
    }

    public String storeEncryptedFile(MultipartFile file, String subDirectory) {
        validateFile(file);
        try {
            Path targetDir = Paths.get(uploadDir).resolve(subDirectory).toAbsolutePath().normalize();
            Files.createDirectories(targetDir);
            String ext = FilenameUtils.getExtension(
                Objects.requireNonNull(file.getOriginalFilename())).toLowerCase();
            String storedName = UUID.randomUUID() + "." + ext + ".enc";
            Path targetPath = targetDir.resolve(storedName);
            byte[] encrypted = encrypt(file.getBytes());
            Files.write(targetPath, encrypted);
            log.info("Encrypted file stored: {}", targetPath);
            return subDirectory + "/" + storedName;
        } catch (Exception ex) {
            throw new FileStorageException("Could not store encrypted file.", ex);
        }
    }

    public byte[] loadFile(String filePath) {
        try {
            Path path = Paths.get(uploadDir).resolve(filePath).normalize();
            if (!Files.exists(path)) throw new FileStorageException("File not found: " + filePath);
            return Files.readAllBytes(path);
        } catch (IOException ex) {
            throw new FileStorageException("Could not read file: " + filePath, ex);
        }
    }

    public byte[] loadDecryptedFile(String filePath) {
        try {
            return decrypt(loadFile(filePath));
        } catch (Exception ex) {
            throw new FileStorageException("Could not decrypt file: " + filePath, ex);
        }
    }

    public void deleteFile(String filePath) {
        try {
            Files.deleteIfExists(Paths.get(uploadDir).resolve(filePath).normalize());
            log.info("File deleted: {}", filePath);
        } catch (IOException ex) {
            log.error("Could not delete file: {}", filePath, ex);
        }
    }

    public String copyFileAsVersion(String originalPath, int versionNumber) {
        try {
            Path source = Paths.get(uploadDir).resolve(originalPath).normalize();
            String ext = FilenameUtils.getExtension(originalPath);
            String vName = UUID.randomUUID() + "_v" + versionNumber + "." + ext;
            Path targetDir = Paths.get(uploadDir).resolve("versions").toAbsolutePath().normalize();
            Files.createDirectories(targetDir);
            Files.copy(source, targetDir.resolve(vName), StandardCopyOption.REPLACE_EXISTING);
            return "versions/" + vName;
        } catch (IOException ex) {
            throw new FileStorageException("Could not create version copy.", ex);
        }
    }

    public String computeChecksum(MultipartFile file) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(file.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception ex) {
            log.warn("Could not compute checksum", ex);
            return null;
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new FileStorageException("File is empty");
        String name = file.getOriginalFilename();
        if (name == null || name.isBlank()) throw new FileStorageException("Invalid filename");
        if (name.contains("..")) throw new FileStorageException("Filename contains invalid path sequence");
        String ext = FilenameUtils.getExtension(name).toLowerCase();
        List<String> allowed = Arrays.asList(allowedTypes.split(","));
        if (!allowed.contains(ext))
            throw new FileStorageException("File type '." + ext + "' not allowed. Allowed: " + allowedTypes);
    }

    private byte[] encrypt(byte[] data) throws Exception {
        SecretKeySpec key = new SecretKeySpec(Arrays.copyOf(encryptionKey.getBytes(), 32), "AES");
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, key);
        return cipher.doFinal(data);
    }

    private byte[] decrypt(byte[] data) throws Exception {
        SecretKeySpec key = new SecretKeySpec(Arrays.copyOf(encryptionKey.getBytes(), 32), "AES");
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, key);
        return cipher.doFinal(data);
    }

    public String getUploadDir() { return uploadDir; }
}
