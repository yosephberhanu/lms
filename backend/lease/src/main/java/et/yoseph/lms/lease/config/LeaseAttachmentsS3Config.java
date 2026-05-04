package et.yoseph.lms.lease.config;

import io.minio.MinioClient;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(LeaseAttachmentsS3Config.LeaseAttachmentsS3Properties.class)
public class LeaseAttachmentsS3Config {

    @Bean
    MinioClient leaseAttachmentsMinioClient(LeaseAttachmentsS3Properties props) {
        return MinioClient.builder()
                .endpoint(props.endpoint())
                .credentials(props.accessKey(), props.secretKey())
                .build();
    }

    @ConfigurationProperties(prefix = "lease.attachments.s3")
    public record LeaseAttachmentsS3Properties(
            String endpoint,
            String accessKey,
            String secretKey,
            String bucket) {
    }
}

