package et.yoseph.lms.keycloakadmin.service;

import et.yoseph.lms.keycloakadmin.integration.KeycloakAdminTokenProvider;
import et.yoseph.lms.keycloakadmin.web.dto.AdminUserResponse;
import et.yoseph.lms.keycloakadmin.web.dto.RealmRoleResponse;
import et.yoseph.lms.keycloakadmin.web.dto.UserCreateRequest;
import et.yoseph.lms.keycloakadmin.web.dto.UserUpdateRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class KeycloakUserAdminService {

    private static final ParameterizedTypeReference<List<Map<String, Object>>> LIST_OF_MAPS =
            new ParameterizedTypeReference<>() {};

    private static final ParameterizedTypeReference<Map<String, Object>> MAP_TYPE =
            new ParameterizedTypeReference<>() {};

    private static final List<String> PHONE_ATTR_KEYS = List.of("phone", "phone_number", "phoneNumber", "mobile");

    private final RestClient adminClient;

    public KeycloakUserAdminService(
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

    public List<AdminUserResponse> list(String search, int first, int max, boolean includeRoles) {
        int safeFirst = Math.max(0, first);
        int safeMax = Math.min(100, Math.max(1, max));
        var spec = adminClient.get()
                .uri(uriBuilder -> {
                    var b = uriBuilder.path("/users")
                            // false = include attributes (e.g. phone); needed for admin user list/details
                            .queryParam("briefRepresentation", "false")
                            .queryParam("first", safeFirst)
                            .queryParam("max", safeMax);
                    if (StringUtils.hasText(search)) {
                        b.queryParam("search", search.trim());
                    }
                    return b.build();
                });
        List<Map<String, Object>> rows;
        try {
            rows = spec.retrieve().body(LIST_OF_MAPS);
        } catch (HttpStatusCodeException e) {
            throw translate(e, "Unable to list users");
        }
        if (rows == null) {
            return List.of();
        }
        List<AdminUserResponse> out = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            if (!includeRoles) {
                // Intentionally omit roles for list() to avoid N+1 Keycloak calls.
                out.add(mapUser(row, List.of()));
                continue;
            }
            String id = row.get("id") != null ? row.get("id").toString() : "";
            List<String> roles = StringUtils.hasText(id) ? userRealmRoleNames(id) : List.of();
            out.add(mapUser(row, roles));
        }
        return out;
    }

    public AdminUserResponse get(String id) {
        try {
            Map<String, Object> row = adminClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/users/{id}")
                            // needed to include attributes (e.g. phone)
                            .queryParam("briefRepresentation", "false")
                            .build(id))
                    .retrieve()
                    .body(MAP_TYPE);
            if (row == null) {
                throw new NotFoundException("User not found: " + id);
            }
            List<String> roles = userRealmRoleNames(id);
            return mapUser(row, roles);
        } catch (HttpStatusCodeException e) {
            if (e.getStatusCode().value() == 404) {
                throw new NotFoundException("User not found: " + id);
            }
            throw translate(e, "Unable to load user");
        }
    }

    public String create(UserCreateRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("username", request.username().trim());
        body.put("enabled", request.enabled());
        if (StringUtils.hasText(request.email())) {
            body.put("email", request.email().trim());
        }
        if (StringUtils.hasText(request.phone())) {
            List<String> v = List.of(request.phone().trim());
            body.put("attributes", Map.of(
                    "phone", v,
                    "phone_number", v,
                    "phoneNumber", v,
                    "mobile", v
            ));
        }
        if (StringUtils.hasText(request.firstName())) {
            body.put("firstName", request.firstName().trim());
        }
        if (StringUtils.hasText(request.lastName())) {
            body.put("lastName", request.lastName().trim());
        }
        if (StringUtils.hasText(request.password())) {
            if (request.password().length() < 4) {
                throw new BadRequestException("Password must be at least 4 characters");
            }
            List<Map<String, Object>> creds = new ArrayList<>();
            Map<String, Object> c = new LinkedHashMap<>();
            c.put("type", "password");
            c.put("value", request.password());
            c.put("temporary", false);
            creds.add(c);
            body.put("credentials", creds);
        }
        try {
            URI location = adminClient.post()
                    .uri("/users")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity()
                    .getHeaders()
                    .getLocation();
            if (location == null) {
                throw new BadRequestException("Keycloak did not return Location for created user");
            }
            String path = location.getPath();
            int slash = path.lastIndexOf('/');
            String id = slash >= 0 ? path.substring(slash + 1) : path;
            if (request.realmRoles() != null) {
                setUserRealmRoles(id, request.realmRoles());
            }
            return id;
        } catch (HttpStatusCodeException e) {
            throw translate(e, "Unable to create user");
        }
    }

    public void update(String id, UserUpdateRequest request) {
        // Build a minimal update payload. Sending the entire fetched Keycloak user representation
        // back on update can cause Keycloak to silently ignore attribute changes in some setups.
        Map<String, Object> body = new LinkedHashMap<>();
        if (request.email() != null) {
            body.put("email", request.email());
        }
        if (request.firstName() != null) {
            body.put("firstName", request.firstName());
        }
        if (request.lastName() != null) {
            body.put("lastName", request.lastName());
        }
        if (request.enabled() != null) {
            body.put("enabled", request.enabled());
        }
        if (request.phone() != null) {
            // Always merge with existing attributes so we don't accidentally wipe attributes and so
            // the outgoing Keycloak representation remains consistent.
            Map<String, Object> existing;
            try {
                existing = adminClient.get()
                        .uri(uriBuilder -> uriBuilder
                                .path("/users/{id}")
                                .queryParam("briefRepresentation", "false")
                                .build(id))
                        .retrieve()
                        .body(MAP_TYPE);
            } catch (HttpStatusCodeException e) {
                if (e.getStatusCode().value() == 404) {
                    throw new NotFoundException("User not found: " + id);
                }
                throw translate(e, "Unable to load user");
            }
            if (existing == null) {
                throw new NotFoundException("User not found: " + id);
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> attrs = existing.get("attributes") instanceof Map<?, ?> m
                    ? new LinkedHashMap<>((Map<String, Object>) m)
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
            body.put("attributes", attrs);
        }
        try {
            adminClient.put()
                    .uri("/users/{id}", id)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
        } catch (HttpStatusCodeException e) {
            throw translate(e, "Unable to update user");
        }
        if (request.realmRoles() != null) {
            setUserRealmRoles(id, request.realmRoles());
        }
        if (StringUtils.hasText(request.password())) {
            if (request.password().length() < 4) {
                throw new BadRequestException("Password must be at least 4 characters");
            }
            Map<String, Object> cred = new LinkedHashMap<>();
            cred.put("type", "password");
            cred.put("value", request.password());
            cred.put("temporary", false);
            try {
                adminClient.put()
                        .uri("/users/{id}/reset-password", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(cred)
                        .retrieve()
                        .toBodilessEntity();
            } catch (HttpStatusCodeException e) {
                throw translate(e, "Unable to reset password");
            }
        }
    }

    public void delete(String id) {
        try {
            adminClient.delete()
                    .uri("/users/{id}", id)
                    .retrieve()
                    .toBodilessEntity();
        } catch (HttpStatusCodeException e) {
            throw translate(e, "Unable to delete user");
        }
    }

    public List<RealmRoleResponse> listRealmRoles(String prefix) {
        List<Map<String, Object>> rows;
        try {
            rows = adminClient.get()
                    .uri("/roles")
                    .retrieve()
                    .body(LIST_OF_MAPS);
        } catch (HttpStatusCodeException e) {
            throw translate(e, "Unable to list roles");
        }
        if (rows == null) {
            return List.of();
        }
        String p = StringUtils.hasText(prefix) ? prefix.trim() : null;
        return rows.stream()
                .map(r -> new RealmRoleResponse(
                        r.get("name") != null ? r.get("name").toString() : "",
                        r.get("description") != null ? r.get("description").toString() : null))
                .filter(r -> StringUtils.hasText(r.name()))
                .filter(r -> p == null || r.name().startsWith(p))
                .sorted(Comparator.comparing(RealmRoleResponse::name))
                .toList();
    }

    private List<Map<String, Object>> userRealmRoleReps(String userId) {
        try {
            List<Map<String, Object>> rows = adminClient.get()
                    .uri("/users/{id}/role-mappings/realm", userId)
                    .retrieve()
                    .body(LIST_OF_MAPS);
            return rows != null ? rows : List.of();
        } catch (HttpStatusCodeException e) {
            throw translate(e, "Unable to load user roles");
        }
    }

    private List<String> userRealmRoleNames(String userId) {
        return userRealmRoleReps(userId).stream()
                .map(r -> r.get("name") != null ? r.get("name").toString() : null)
                .filter(Objects::nonNull)
                .filter(StringUtils::hasText)
                .sorted()
                .toList();
    }

    private void setUserRealmRoles(String userId, List<String> desiredNamesRaw) {
        List<String> desiredNames = desiredNamesRaw == null ? List.of() : desiredNamesRaw.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(StringUtils::hasText)
                .distinct()
                .toList();

        List<Map<String, Object>> current = userRealmRoleReps(userId);
        Set<String> currentNames = current.stream()
                .map(r -> r.get("name") != null ? r.get("name").toString() : null)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Set<String> desiredSet = Set.copyOf(desiredNames);
        Set<String> toRemove = currentNames.stream()
                .filter(n -> !desiredSet.contains(n))
                .collect(Collectors.toSet());
        Set<String> toAdd = desiredSet.stream()
                .filter(n -> !currentNames.contains(n))
                .collect(Collectors.toSet());

        if (!toRemove.isEmpty()) {
            List<Map<String, Object>> reps = current.stream()
                    .filter(r -> r.get("name") != null && toRemove.contains(r.get("name").toString()))
                    .toList();
            try {
                adminClient.method(HttpMethod.DELETE)
                        .uri("/users/{id}/role-mappings/realm", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(reps)
                        .retrieve()
                        .toBodilessEntity();
            } catch (HttpStatusCodeException e) {
                throw translate(e, "Unable to remove user roles");
            }
        }

        if (!toAdd.isEmpty()) {
            // role reps must include id+name at minimum; fetch by role-name.
            List<Map<String, Object>> reps = toAdd.stream()
                    .map(this::realmRoleRepByName)
                    .toList();
            try {
                adminClient.post()
                        .uri("/users/{id}/role-mappings/realm", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(reps)
                        .retrieve()
                        .toBodilessEntity();
            } catch (HttpStatusCodeException e) {
                throw translate(e, "Unable to add user roles");
            }
        }
    }

    private Map<String, Object> realmRoleRepByName(String roleName) {
        try {
            Map<String, Object> rep = adminClient.get()
                    .uri("/roles/{roleName}", roleName)
                    .retrieve()
                    .body(MAP_TYPE);
            if (rep == null) {
                throw new BadRequestException("Unknown role: " + roleName);
            }
            return rep;
        } catch (HttpStatusCodeException e) {
            if (e.getStatusCode().value() == 404) {
                throw new BadRequestException("Unknown role: " + roleName);
            }
            throw translate(e, "Unable to load role: " + roleName);
        }
    }

    private static AdminUserResponse mapUser(Map<String, Object> row, List<String> realmRoles) {
        String id = row.get("id") != null ? row.get("id").toString() : "";
        String username = row.get("username") != null ? row.get("username").toString() : "";
        String email = row.get("email") != null ? row.get("email").toString() : null;
        String phone = null;
        if (row.get("attributes") instanceof Map<?, ?> attrs) {
            phone = firstAttr(attrs, PHONE_ATTR_KEYS);
        }
        String firstName = row.get("firstName") != null ? row.get("firstName").toString() : null;
        String lastName = row.get("lastName") != null ? row.get("lastName").toString() : null;
        boolean enabled = row.get("enabled") instanceof Boolean b && b;
        return new AdminUserResponse(id, username, email, phone, firstName, lastName, realmRoles != null ? realmRoles : List.of(), enabled);
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

    private static RuntimeException translate(HttpStatusCodeException e, String fallback) {
        HttpStatusCode status = e.getStatusCode();
        String msg = fallback;
        try {
            Map<String, Object> n = e.getResponseBodyAs(MAP_TYPE);
            if (n != null) {
                Object em = n.get("errorMessage");
                if (em != null) {
                    msg = em.toString();
                } else {
                    Object ed = n.get("error_description");
                    if (ed != null) {
                        msg = ed.toString();
                    }
                }
            }
        } catch (Exception ignored) {
            // keep fallback
        }
        if (status.value() == 404) {
            return new NotFoundException(msg);
        }
        return new BadRequestException(msg);
    }

    private static String trimSlash(String u) {
        return u.endsWith("/") ? u.substring(0, u.length() - 1) : u;
    }
}
