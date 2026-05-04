package et.yoseph.lms.keycloakadmin.integration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.Map;

/**
 * Obtains a short-lived bearer token for the Keycloak Admin REST API using the {@code admin-cli} client
 * against the {@code master} realm (bootstrap admin user).
 */
@Component
public class KeycloakAdminTokenProvider {

    private static final ParameterizedTypeReference<Map<String, Object>> TOKEN_JSON =
            new ParameterizedTypeReference<>() {};

    private final RestClient tokenClient;
    private final String adminUsername;
    private final String adminPassword;

    private volatile String cachedToken;
    private volatile Instant tokenExpiresAt = Instant.EPOCH;

    public KeycloakAdminTokenProvider(
            @Value("${lms.keycloak.base-url}") String keycloakBaseUrl,
            @Value("${lms.keycloak.admin-username}") String adminUsername,
            @Value("${lms.keycloak.admin-password}") String adminPassword) {
        String root = keycloakBaseUrl.endsWith("/") ? keycloakBaseUrl.substring(0, keycloakBaseUrl.length() - 1) : keycloakBaseUrl;
        this.tokenClient = RestClient.builder().baseUrl(root).build();
        this.adminUsername = adminUsername;
        this.adminPassword = adminPassword;
    }

    public String bearerAccessToken() {
        Instant now = Instant.now();
        if (cachedToken != null && now.isBefore(tokenExpiresAt.minusSeconds(30))) {
            return cachedToken;
        }
        synchronized (this) {
            if (cachedToken != null && now.isBefore(tokenExpiresAt.minusSeconds(30))) {
                return cachedToken;
            }
            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("grant_type", "password");
            form.add("client_id", "admin-cli");
            form.add("username", adminUsername);
            form.add("password", adminPassword);
            Map<String, Object> body = tokenClient.post()
                    .uri("/realms/master/protocol/openid-connect/token")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(TOKEN_JSON);
            if (body == null || body.get("access_token") == null) {
                throw new IllegalStateException("Keycloak token response missing access_token");
            }
            cachedToken = body.get("access_token").toString();
            Object expiresObj = body.get("expires_in");
            long expiresIn = 60;
            if (expiresObj instanceof Number n) {
                expiresIn = n.longValue();
            }
            tokenExpiresAt = now.plusSeconds(expiresIn);
            return cachedToken;
        }
    }
}
