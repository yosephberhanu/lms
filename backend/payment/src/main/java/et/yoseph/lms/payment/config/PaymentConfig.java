package et.yoseph.lms.payment.config;

import et.yoseph.lms.payment.service.StripePaymentProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(StripePaymentProperties.class)
public class PaymentConfig {
}

