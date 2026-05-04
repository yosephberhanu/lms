package et.yoseph.lms.keycloakadmin.web.dto;

import java.util.List;

public record AdminUserResponse(
        String id,
        String username,
        String email,
        String phone,
        String firstName,
        String lastName,
        List<String> realmRoles,
        boolean enabled) {
}
