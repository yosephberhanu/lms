package et.yoseph.lms.notification.service;

import et.yoseph.lms.notification.integration.KeycloakAdminTokenProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Resolves a user's contact info + notification prefs from Keycloak.
 *
 * <p>In this codebase, {@code userSub} is typically the Keycloak username (preferred_username),
 * not the UUID id.</p>
 */
@Service
public class UserContactPreferencesResolver {
    private static final ParameterizedTypeReference<List<Map<String, Object>>> LIST_OF_MAPS =
            new ParameterizedTypeReference<>() {};

    private static final String ATTR_NOTIFY_EMAIL = "lms_notify_email";
    private static final String ATTR_NOTIFY_SMS = "lms_notify_sms";
    private static final List<String> PHONE_ATTR_KEYS = List.of("phone", "phone_number", "phoneNumber", "mobile");

    private final RestClient adminClient;
    private final UserCommunicationPreferenceService storedPrefs;

    public record UserCommPrefs(
            String username,
            String email,
            String phone,
            boolean notifyEmail,
            boolean notifySms
    ) {}

    public UserContactPreferencesResolver(
            @Value("${lms.keycloak.base-url}") String keycloakBaseUrl,
            @Value("${lms.keycloak.realm}") String realm,
            KeycloakAdminTokenProvider tokenProvider,
            UserCommunicationPreferenceService storedPrefs) {
        String root = trimSlash(keycloakBaseUrl) + "/admin/realms/" + realm;
        this.adminClient = RestClient.builder()
                .baseUrl(root)
                .requestInterceptor((request, body, execution) -> {
                    request.getHeaders().setBearerAuth(tokenProvider.bearerAccessToken());
                    return execution.execute(request, body);
                })
                .build();
        this.storedPrefs = storedPrefs;
    }

    public Optional<UserCommPrefs> resolveByUserSub(String userSub) {
        if (!StringUtils.hasText(userSub)) {
            return Optional.empty();
        }
        // Use the search/list endpoint; it is the most stable for our current Keycloak setup.
        List<Map<String, Object>> rows;
        try {
            rows = adminClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/users")
                            .queryParam("username", userSub.trim())
                            .queryParam("exact", true)
                            .build())
                    .retrieve()
                    .body(LIST_OF_MAPS);
        } catch (HttpStatusCodeException e) {
            HttpStatusCode st = e.getStatusCode();
            if (st.value() == 404) {
                return Optional.empty();
            }
            throw new IllegalStateException("Unable to resolve Keycloak user " + userSub + ": " + st, e);
        }
        if (rows == null || rows.isEmpty()) {
            return Optional.empty();
        }
        Map<String, Object> u = rows.getFirst();

        String username = str(u.get("username")).orElse(userSub.trim());
        String email = str(u.get("email")).orElse(null);
        String phone = null;
        boolean notifyEmail = false;
        boolean notifySms = false;
        if (u.get("attributes") instanceof Map<?, ?> attrs) {
            phone = firstPhone(attrs).orElse(null);
            notifyEmail = readBoolAttr(attrs, ATTR_NOTIFY_EMAIL);
            notifySms = readBoolAttr(attrs, ATTR_NOTIFY_SMS);
        }

        // Prefer explicit preferences stored in notification DB (SPA saves here).
        var stored = storedPrefs.find(username);
        if (stored.isPresent()) {
            notifyEmail = stored.get().isNotifyEmail();
            notifySms = stored.get().isNotifySms();
            if (StringUtils.hasText(stored.get().getEmail())) {
                email = stored.get().getEmail();
            }
            if (StringUtils.hasText(stored.get().getPhone())) {
                phone = stored.get().getPhone();
            }
        }
        return Optional.of(new UserCommPrefs(username, email, phone, notifyEmail, notifySms));
    }

    private static boolean readBoolAttr(Map<?, ?> attrs, String key) {
        return firstAttr(attrs, key).map(v -> "true".equalsIgnoreCase(v)).orElse(false);
    }

    private static Optional<String> firstAttr(Map<?, ?> attrs, String key) {
        Object v = attrs.get(key);
        if (v instanceof List<?> list && !list.isEmpty() && list.getFirst() != null) {
            return Optional.of(list.getFirst().toString());
        }
        if (v instanceof String s && StringUtils.hasText(s)) {
            return Optional.of(s);
        }
        return Optional.empty();
    }

    private static Optional<String> firstPhone(Map<?, ?> attrs) {
        for (String key : PHONE_ATTR_KEYS) {
            Optional<String> v = firstAttr(attrs, key).filter(StringUtils::hasText);
            if (v.isPresent()) {
                return v;
            }
        }
        return Optional.empty();
    }

    private static Optional<String> str(Object o) {
        if (o == null) return Optional.empty();
        String s = o.toString();
        return StringUtils.hasText(s) ? Optional.of(s) : Optional.empty();
    }

    private static String trimSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}

