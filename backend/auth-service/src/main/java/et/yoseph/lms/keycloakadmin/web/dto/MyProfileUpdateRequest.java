package et.yoseph.lms.keycloakadmin.web.dto;

import jakarta.validation.constraints.Size;

public record MyProfileUpdateRequest(
        @Size(max = 255) String email,
        @Size(max = 64) String phone,
        @Size(max = 128) String firstName,
        @Size(max = 128) String lastName,
        Boolean notifyEmail,
        Boolean notifySms
) {
}

