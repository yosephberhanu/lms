package et.yoseph.lms.keycloakadmin.config;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Maps Keycloak realm roles and client roles to Spring {@code GrantedAuthority} with {@code ROLE_} prefix.
 * <p>
 * Reads {@code realm_access.roles} and every {@code resource_access.*.roles} list. Some Keycloak setups
 * omit {@code realm_access} on access tokens unless the client has full scope; client role lists may still
 * carry effective roles.
 */
public class JwtRealmRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        Set<String> roleNames = new LinkedHashSet<>();
        collectFromRealmAccess(jwt, roleNames);
        collectFromResourceAccess(jwt, roleNames);
        List<GrantedAuthority> out = new ArrayList<>(roleNames.size());
        for (String r : roleNames) {
            out.add(new SimpleGrantedAuthority("ROLE_" + r));
        }
        return out;
    }

    private static void collectFromRealmAccess(Jwt jwt, Set<String> roleNames) {
        Map<String, Object> realmAccess = jwt.getClaim("realm_access");
        if (realmAccess == null) {
            return;
        }
        addRoles(realmAccess.get("roles"), roleNames);
    }

    private static void collectFromResourceAccess(Jwt jwt, Set<String> roleNames) {
        Map<String, Object> resourceAccess = jwt.getClaim("resource_access");
        if (resourceAccess == null) {
            return;
        }
        for (Object clientEntryObj : resourceAccess.values()) {
            if (!(clientEntryObj instanceof Map<?, ?> clientEntry)) {
                continue;
            }
            addRoles(clientEntry.get("roles"), roleNames);
        }
    }

    private static void addRoles(Object rolesObj, Set<String> roleNames) {
        if (!(rolesObj instanceof Collection<?> roles)) {
            return;
        }
        for (Object r : roles) {
            if (r != null) {
                roleNames.add(r.toString());
            }
        }
    }
}
