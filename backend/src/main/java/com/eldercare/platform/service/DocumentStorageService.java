package com.eldercare.platform.service;

import com.eldercare.platform.exception.BadRequestException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Set;
import java.util.UUID;

@Service
@Slf4j
public class DocumentStorageService {

    private static final long MAX_FILE_SIZE = 20 * 1024 * 1024; // 20 MB
    private static final Set<String> ALLOWED_TYPES = Set.of("application/pdf", "image/jpeg", "image/png");
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("pdf", "jpg", "jpeg", "png");

    @Value("${application.document-storage.path:./uploads/medical-documents}")
    private String storagePath;

    public StorageResult store(MultipartFile file, UUID elderlyUserId) throws IOException {
        validateFile(file);

        String originalName = file.getOriginalFilename();
        String extension = getExtension(originalName);
        String storageKey = "docs/" + elderlyUserId + "/" + UUID.randomUUID() + "." + extension;

        Path destination = Path.of(storagePath).resolve(storageKey).normalize();

        // Path traversal protection
        if (!destination.startsWith(Path.of(storagePath).normalize())) {
            throw new BadRequestException("Invalid storage path");
        }

        Files.createDirectories(destination.getParent());

        byte[] bytes = file.getBytes();
        Files.write(destination, bytes, StandardOpenOption.CREATE_NEW);

        String checksum = computeSha256(bytes);

        log.info("Stored document for elderly {} at key {}", elderlyUserId, storageKey);

        return new StorageResult(
                storageKey,
                originalName,
                extension.toUpperCase(),
                file.getSize(),
                checksum
        );
    }

    public byte[] retrieve(String storageKey) throws IOException {
        Path file = resolveAndValidatePath(storageKey);
        if (!Files.exists(file)) {
            throw new BadRequestException("Document file not found");
        }
        return Files.readAllBytes(file);
    }

    public void delete(String storageKey) throws IOException {
        Path file = resolveAndValidatePath(storageKey);
        if (Files.exists(file)) {
            Files.delete(file);
            log.info("Deleted document at key {}", storageKey);
        }
    }

    private Path resolveAndValidatePath(String storageKey) {
        Path base = Path.of(storagePath).normalize().toAbsolutePath();
        Path target = base.resolve(storageKey).normalize();
        if (!target.startsWith(base)) {
            throw new BadRequestException("Invalid storage key");
        }
        return target;
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("No file provided");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BadRequestException("File exceeds maximum size of 20MB");
        }

        String originalName = file.getOriginalFilename();
        if (originalName == null || originalName.isBlank()) {
            throw new BadRequestException("File name is missing");
        }

        String ext = getExtension(originalName).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            throw new BadRequestException("File type not allowed. Supported: PDF, JPG, JPEG, PNG");
        }

        String contentType = file.getContentType();
        if (contentType != null && !ALLOWED_TYPES.contains(contentType.toLowerCase())) {
            // Check based on extension as a fallback; content-type can be unreliable from clients
            if (!ext.equals("jpg") && !ext.equals("jpeg") && !ext.equals("png") && !ext.equals("pdf")) {
                throw new BadRequestException("File MIME type not permitted");
            }
        }
    }

    private String getExtension(String fileName) {
        if (fileName == null) return "";
        int dot = fileName.lastIndexOf('.');
        return dot >= 0 ? fileName.substring(dot + 1).toLowerCase() : "";
    }

    private String computeSha256(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(data));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    public record StorageResult(
            String storageKey,
            String originalFileName,
            String fileFormat,
            long fileSizeBytes,
            String sha256Checksum
    ) {}
}
