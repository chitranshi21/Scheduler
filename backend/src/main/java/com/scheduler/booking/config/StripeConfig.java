package com.scheduler.booking.config;

import com.stripe.Stripe;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "stripe")
@Data
public class StripeConfig {

    private String secretKey;
    private String publishableKey;
    private String webhookSecret;
    private boolean enabled = true;
    private Double platformFeePercentage = 5.0; // 5% management fee

    @PostConstruct
    public void init() {
        if (enabled && secretKey != null && !secretKey.isEmpty()) {
            Stripe.apiKey = secretKey;
        }
    }

    public String getWebhookSecret() {
        return webhookSecret;
    }

    public Double getPlatformFeePercentage() {
        return platformFeePercentage;
    }
}
