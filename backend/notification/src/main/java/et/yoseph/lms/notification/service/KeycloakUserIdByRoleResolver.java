package et.yoseph.lms.notification.service;

import et.yoseph.lms.notification.integration.KeycloakAdminTokenProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class KeycloakUserIdByRoleResolver {

    private static final ParameterizedTypeReference<List<Map<String, Object>>> LIST_OF_MAPS =
            new ParameterizedTypeReference<>() {};

    private final RestClient adminClient;

    public KeycloakUserIdByRoleResolver(
            @Value("${lms.keycloak.base-url}") String keycloakBaseUrl,
            @Value("${lms.keycloak.realm}") String realm,
            KeycloakAdminTokenProvider tokenProvider) {
        String root = trimSlash(keycloakBaseUrl) + "/admin/realms/" + realm;
        this.adminClient = RestClient.builder()
                .baseUrl(root)
                .requestInterceptor((request, body, execution) -> {
                    request.getHeaders().setBearerAuth(tokenProvider.bearerAccessToken());
                    return execution.execute(request, body);
                })
                .build();
    }

    /**
     * Returns distinct user identifiers for recipients that have any of the given realm role names.
     *
     * <p>We intentionally use Keycloak usernames (not UUID user ids) because the SPA tokens and SSE routing
     * in this project use {@code preferred_username} / username as the stable user key.</p>
     */
    public Set<String> userIdsForRealmRoles(List<String> realmRoleNames) {
        Set<String> ids = new LinkedHashSet<>();
        for (String role : realmRoleNames) {
            if (!StringUtils.hasText(role)) {
                continue;
            }
            String trimmed = role.trim();
            int first = 0;
            final int pageSize = 100;
            while (true) {
                List<Map<String, Object>> page = fetchUsersForRole(trimmed, first, pageSize);
                if (page == null || page.isEmpty()) {
                    break;
                }
                for (Map<String, Object> row : page) {
                    Object username = row.get("username");
                    if (username != null && StringUtils.hasText(username.toString())) {
                        ids.add(username.toString());
                        continue;
                    }
                    // Fallback (shouldn't happen): Keycloak also returns the UUID user id field as "id".
                    Object id = row.get("id");
                    if (id != null && StringUtils.hasText(id.toString())) {
                        ids.add(id.toString());
                    }
                }
                if (page.size() < pageSize) {
                    break;
                }
                first += pageSize;
            }
        }
        return ids;
    }

    private List<Map<String, Object>> fetchUsersForRole(String roleName, int first, int max) {
        try {
            List<Map<String, Object>> rows = adminClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/roles/{roleName}/users")
                            .queryParam("first", first)
                            .queryParam("max", max)
                            .build(roleName))
                    .retrieve()
                    .body(LIST_OF_MAPS);
            return rows != null ? rows : List.of();
        } catch (HttpStatusCodeException e) {
            HttpStatusCode st = e.getStatusCode();
            if (st.value() == 404) {
                return List.of();
            }
            throw new IllegalStateException("Unable to list Keycloak users for role " + roleName + ": " + st, e);
        }
    }

    private static String trimSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
