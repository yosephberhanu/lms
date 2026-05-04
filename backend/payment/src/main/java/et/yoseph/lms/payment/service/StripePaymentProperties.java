package et.yoseph.lms.payment.service;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "payment.stripe")
public record StripePaymentProperties(
        String secretKey,
        String webhookSecret,
        String currency,
        String successUrlTemplate,
        String cancelUrlTemplate
) {
}

