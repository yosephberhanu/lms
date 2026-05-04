package et.yoseph.lms.payment.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Configuration
public class PaymentOutboundHttpConfig {

    @Bean
    ClientHttpRequestInterceptor lmsAuthorizationPropagationInterceptor() {
        return (request, body, execution) -> {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                String auth = attrs.getRequest().getHeader(HttpHeaders.AUTHORIZATION);
                if (StringUtils.hasText(auth)) {
                    request.getHeaders().set(HttpHeaders.AUTHORIZATION, auth);
                }
            }
            return execution.execute(request, body);
        };
    }

    @Bean
    RestClient leaseServiceRestClient(
            @Value("${payment.lease-service.base-url}") String baseUrl,
            RestClient.Builder builder,
            ClientHttpRequestInterceptor lmsAuthorizationPropagationInterceptor) {
        return builder
                .baseUrl(baseUrl)
                .requestInterceptor(lmsAuthorizationPropagationInterceptor)
                .build();
    }
}

