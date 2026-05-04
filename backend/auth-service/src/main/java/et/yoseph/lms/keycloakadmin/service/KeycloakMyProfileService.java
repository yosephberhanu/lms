package et.yoseph.lms.keycloakadmin.service;

import et.yoseph.lms.keycloakadmin.integration.KeycloakAdminTokenProvider;
import et.yoseph.lms.keycloakadmin.web.dto.MyProfileResponse;
import et.yoseph.lms.keycloakadmin.web.dto.MyProfileUpdateRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClient;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class KeycloakMyProfileService {

    private static final ParameterizedTypeReference<Map<String, Object>> MAP_TYPE =
            new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<List<Map<String, Object>>> LIST_OF_MAP_TYPE =
            new ParameterizedTypeReference<>() {};

    private static final String ATTR_NOTIFY_EMAIL = "lms_notify_email";
    private static final String ATTR_NOTIFY_SMS = "lms_notify_sms";
    /**
     * Keycloak realms can differ in which attribute key they retain for phone numbers
     * (especially when "user profile" schemas are enabled). We write and read a small set of
     * common keys to make the profile page behave consistently across realms.
     */
    private static final List<String> PHONE_ATTR_KEYS = List.of("phone", "phone_number", "phoneNumber", "mobile");

    private final RestClient adminClient;

    public KeycloakMyProfileService(
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

    public MyProfileResponse me(Jwt jwt) {
        Map<String, Object> row;
        try {
            row = loadUserRow(jwt);
        } catch (HttpStatusCodeException e) {
            throw new BadRequestException("Unable to load user profile");
        }
        if (row == null) {
            throw new NotFoundException("User not found");
        }
        return mapProfile(row);
    }

    public MyProfileResponse updateMe(Jwt jwt, MyProfileUpdateRequest request) {
        Map<String, Object> existing;
        try {
            existing = loadUserRow(jwt);
        } catch (HttpStatusCodeException e) {
            throw new BadRequestException("Unable to load user profile");
        }
        if (existing == null) {
            throw new NotFoundException("User not found");
        }

        if (request.email() != null) {
            existing.put("email", request.email());
        }
        if (request.firstName() != null) {
            existing.put("firstName", request.firstName());
        }
        if (request.lastName() != null) {
            existing.put("lastName", request.lastName());
        }
        if (request.phone() != null) {
            @SuppressWarnings("unchecked")
            Map<String, Object> attrs = existing.get("attributes") instanceof Map<?, ?> m
                    ? (Map<String, Object>) m
                    : new LinkedHashMap<>();
            if (StringUtils.hasText(request.phone())) {
                List<String> v = List.of(request.phone().trim());
                for (String k : PHONE_ATTR_KEYS) {
                    attrs.put(k, v);
                }
            } else {
                for (String k : PHONE_ATTR_KEYS) {
                    attrs.remove(k);
                }
            }
            existing.put("attributes", attrs);
        }

        if (request.notifyEmail() != null || request.notifySms() != null) {
            @SuppressWarnings("unchecked")
            Map<String, Object> attrs = existing.get("attributes") instanceof Map<?, ?> m
                    ? (Map<String, Object>) m
                    : new LinkedHashMap<>();

            if (request.notifyEmail() != null) {
                setBoolAttr(attrs, ATTR_NOTIFY_EMAIL, request.notifyEmail());
            }
            if (request.notifySms() != null) {
                setBoolAttr(attrs, ATTR_NOTIFY_SMS, request.notifySms());
            }
            existing.put("attributes", attrs);
        }

        try {
            adminClient.put()
                    .uri("/users/{id}", existing.get("id"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(existing)
                    .retrieve()
                    .toBodilessEntity();
        } catch (HttpStatusCodeException e) {
            throw new BadRequestException("Unable to update user profile");
        }

        // Return persisted state (Keycloak may drop attributes not allowed by user-profile, etc.).
        Map<String, Object> fresh;
        try {
            fresh = adminClient.get()
                    .uri("/users/{id}", existing.get("id"))
                    .retrieve()
                    .body(MAP_TYPE);
        } catch (HttpStatusCodeException e) {
            throw new BadRequestException("Unable to load user profile after update");
        }
        if (fresh == null) {
            throw new NotFoundException("User not found");
        }
        return mapProfile(fresh);
    }

    private Map<String, Object> loadUserRow(Jwt jwt) {
        String userId = jwt.getSubject();
        if (StringUtils.hasText(userId)) {
            return adminClient.get()
                    .uri("/users/{id}", userId)
                    .retrieve()
                    .body(MAP_TYPE);
        }

        // Some token flows/scopes can omit `sub`. Fall back to username lookup.
        String username = jwt.getClaimAsString("preferred_username");
        if (!StringUtils.hasText(username)) {
            return null;
        }
        List<Map<String, Object>> rows = adminClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/users")
                        .queryParam("username", username)
                        .queryParam("exact", true)
                        .build())
                .retrieve()
                .body(LIST_OF_MAP_TYPE);
        if (rows == null || rows.isEmpty()) {
            return null;
        }
        Map<String, Object> summary = rows.getFirst();
        Object idObj = summary.get("id");
        if (idObj == null) {
            return summary;
        }
        // The list endpoint can omit fields like `attributes`; fetch the full representation.
        return adminClient.get()
                .uri("/users/{id}", idObj)
                .retrieve()
                .body(MAP_TYPE);
    }

    private static MyProfileResponse mapProfile(Map<String, Object> row) {
        String id = row.get("id") != null ? row.get("id").toString() : "";
        String username = row.get("username") != null ? row.get("username").toString() : "";
        String email = row.get("email") != null ? row.get("email").toString() : null;
        String phone = null;
        boolean notifyEmail = false;
        boolean notifySms = false;
        if (row.get("attributes") instanceof Map<?, ?> attrs) {
            phone = firstAttr(attrs, PHONE_ATTR_KEYS);
            notifyEmail = readBoolAttr(attrs, ATTR_NOTIFY_EMAIL);
            notifySms = readBoolAttr(attrs, ATTR_NOTIFY_SMS);
        }
        String firstName = row.get("firstName") != null ? row.get("firstName").toString() : null;
        String lastName = row.get("lastName") != null ? row.get("lastName").toString() : null;
        return new MyProfileResponse(id, username, email, phone, firstName, lastName, notifyEmail, notifySms);
    }

    private static String trimSlash(String u) {
        return u.endsWith("/") ? u.substring(0, u.length() - 1) : u;
    }

    private static boolean readBoolAttr(Map<?, ?> attrs, String key) {
        String v = firstAttr(attrs, List.of(key));
        return v != null && "true".equalsIgnoreCase(v);
    }

    private static String firstAttr(Map<?, ?> attrs, List<String> keys) {
        for (String key : keys) {
            Object v = attrs.get(key);
            if (v instanceof List<?> list && !list.isEmpty() && list.getFirst() != null) {
                String s = list.getFirst().toString();
                if (StringUtils.hasText(s)) return s;
            }
            if (v instanceof String s && StringUtils.hasText(s)) {
                return s;
            }
        }
        return null;
    }

    private static void setBoolAttr(Map<String, Object> attrs, String key, boolean enabled) {
        if (enabled) {
            attrs.put(key, List.of("true"));
        } else {
            attrs.remove(key);
        }
    }
}

