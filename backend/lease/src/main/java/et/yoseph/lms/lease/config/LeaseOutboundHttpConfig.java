package et.yoseph.lms.lease.config;

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
public class LeaseOutboundHttpConfig {

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
    RestClient propertyServiceRestClient(
            @Value("${lease.property-service.base-url}") String baseUrl,
            ClientHttpRequestInterceptor lmsAuthorizationPropagationInterceptor) {
        return RestClient.builder()
                .baseUrl(trimTrailingSlash(baseUrl))
                .requestInterceptor(lmsAuthorizationPropagationInterceptor)
                .build();
    }

    @Bean
    RestClient paymentServiceRestClient(
            @Value("${lease.payment-service.base-url}") String baseUrl,
            ClientHttpRequestInterceptor lmsAuthorizationPropagationInterceptor) {
        return RestClient.builder()
                .baseUrl(trimTrailingSlash(baseUrl))
                .requestInterceptor(lmsAuthorizationPropagationInterceptor)
                .build();
    }

    private static String trimTrailingSlash(String baseUrl) {
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }
}
