package com.scheduler.booking.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

import java.net.URI;

@Configuration
@ConfigurationProperties(prefix = "cloudflare.r2")
@Data
public class R2Config {

    private String accessKeyId;
    private String secretAccessKey;
    private String accountId;
    private String bucketName;
    private String region = "auto";
    private String publicUrlBase;

    @Bean
    public S3Client s3Client() {
        // Cloudflare R2 endpoint format: https://<account-id>.r2.cloudflarestorage.com
        String endpoint = String.format("https://%s.r2.cloudflarestorage.com", accountId);

        return S3Client.builder()
                .region(Region.of(region))
                .endpointOverride(URI.create(endpoint))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKeyId, secretAccessKey)
                ))
                .build();
    }

    public String getBucketName() {
        return bucketName;
    }

    public String getPublicUrlBase() {
        return publicUrlBase;
    }
}
