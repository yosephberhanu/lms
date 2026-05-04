package et.yoseph.lms.keycloakadmin.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record UserCreateRequest(
        @NotBlank @Size(max = 128) String username,
        @Size(max = 255) String email,
        @Size(max = 64) String phone,
        @Size(max = 128) String firstName,
        @Size(max = 128) String lastName,
        @Size(max = 128) String password,
        List<@Size(max = 128) String> realmRoles,
        boolean enabled) {
}
