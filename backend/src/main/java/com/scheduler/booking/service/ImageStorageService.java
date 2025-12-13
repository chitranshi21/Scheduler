package com.scheduler.booking.service;

import com.scheduler.booking.config.R2Config;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ImageStorageService {

    private final S3Client s3Client;
    private final R2Config r2Config;

    private static final List<String> ALLOWED_CONTENT_TYPES = Arrays.asList(
            "image/jpeg",
            "image/jpg",
            "image/png",
            "image/webp"
    );

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB

    /**
     * Upload an image to Cloudflare R2
     * @param file The image file to upload
     * @param tenantId The tenant ID (for organizing files)
     * @return The public URL of the uploaded image
     */
    public String uploadImage(MultipartFile file, UUID tenantId) {
        try {
            // Validate file
            validateImage(file);

            // Generate unique filename
            String originalFilename = file.getOriginalFilename();
            String fileExtension = getFileExtension(originalFilename);
            String filename = String.format("tenants/%s/%s%s",
                tenantId,
                UUID.randomUUID(),
                fileExtension
            );

            log.info("Uploading image to R2: bucket={}, key={}", r2Config.getBucketName(), filename);

            // Upload to R2
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(r2Config.getBucketName())
                    .key(filename)
                    .contentType(file.getContentType())
                    .cacheControl("public, max-age=31536000") // Cache for 1 year
                    .build();

            s3Client.putObject(
                    putObjectRequest,
                    RequestBody.fromBytes(file.getBytes())
            );

            // Construct public URL
            String publicUrl = String.format("%s/%s", r2Config.getPublicUrlBase(), filename);
            log.info("✅ Image uploaded successfully: {}", publicUrl);

            return publicUrl;

        } catch (IOException e) {
            log.error("Failed to upload image to R2", e);
            throw new RuntimeException("Failed to upload image: " + e.getMessage(), e);
        }
    }

    /**
     * Delete an image from R2
     * @param imageUrl The public URL of the image to delete
     */
    public void deleteImage(String imageUrl) {
        try {
            if (imageUrl == null || imageUrl.isEmpty()) {
                return;
            }

            // Extract the key from the public URL
            String key = extractKeyFromUrl(imageUrl);
            if (key == null) {
                log.warn("Could not extract key from URL: {}", imageUrl);
                return;
            }

            log.info("Deleting image from R2: bucket={}, key={}", r2Config.getBucketName(), key);

            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(r2Config.getBucketName())
                    .key(key)
                    .build();

            s3Client.deleteObject(deleteObjectRequest);
            log.info("✅ Image deleted successfully: {}", key);

        } catch (Exception e) {
            log.error("Failed to delete image from R2: " + imageUrl, e);
            // Don't throw exception - deletion failure shouldn't block updates
        }
    }

    /**
     * Validate the uploaded image file
     */
    private void validateImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        // Check file size
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException(
                String.format("File size exceeds maximum limit of %d MB", MAX_FILE_SIZE / (1024 * 1024))
            );
        }

        // Check content type
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException(
                "Invalid file type. Allowed types: " + String.join(", ", ALLOWED_CONTENT_TYPES)
            );
        }
    }

    /**
     * Extract file extension from filename
     */
    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf("."));
    }

    /**
     * Extract the R2 key from the public URL
     */
    private String extractKeyFromUrl(String url) {
        if (url == null || !url.startsWith(r2Config.getPublicUrlBase())) {
            return null;
        }
        return url.substring(r2Config.getPublicUrlBase().length() + 1);
    }
}
