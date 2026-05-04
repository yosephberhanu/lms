package et.yoseph.lms.lease.integration;

import com.fasterxml.jackson.databind.JsonNode;
import et.yoseph.lms.lease.service.BadRequestException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClient;

/**
 * Optional REST client for the User service (spec: Lease → User). Skips validation when base URL is unset.
 */
@Component
public class UserClient {

    private final RestClient restClientOrNull;

    public UserClient(
            @Value("${lease.user-service.base-url:}") String baseUrl,
            ClientHttpRequestInterceptor lmsAuthorizationPropagationInterceptor) {
        if (!StringUtils.hasText(baseUrl)) {
            this.restClientOrNull = null;
        } else {
            String root = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
            this.restClientOrNull = RestClient.builder()
                    .baseUrl(root)
                    .requestInterceptor(lmsAuthorizationPropagationInterceptor)
                    .build();
        }
    }

    /**
     * Ensures tenant exists when User Service URL is configured.
     */
    public void assertTenantExists(String tenantId) {
        if (restClientOrNull == null) {
            return;
        }
        try {
            restClientOrNull.get()
                    .uri("/users/{id}", tenantId)
                    .retrieve()
                    .toBodilessEntity();
        } catch (HttpStatusCodeException e) {
            HttpStatusCode status = e.getStatusCode();
            if (status.value() == 404) {
                throw new BadRequestException("Tenant user not found: " + tenantId);
            }
            throw new BadRequestException("User service error: " + e.getMessage());
        }
    }

    /**
     * Best-effort display name for snapshot when User Service exposes GET /users/{id}.
     */
    public String fetchTenantDisplayName(String tenantId) {
        if (restClientOrNull == null) {
            return "";
        }
        try {
            JsonNode root = restClientOrNull.get()
                    .uri("/users/{id}", tenantId)
                    .retrieve()
                    .body(JsonNode.class);
            if (root == null) {
                return "";
            }
            if (root.hasNonNull("fullName")) {
                return root.get("fullName").asText();
            }
            if (root.hasNonNull("email")) {
                return root.get("email").asText();
            }
            return "";
        } catch (HttpStatusCodeException e) {
            return "";
        }
    }
}
