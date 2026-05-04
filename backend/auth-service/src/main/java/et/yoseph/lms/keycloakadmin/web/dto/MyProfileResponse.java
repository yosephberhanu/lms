package et.yoseph.lms.keycloakadmin.web.dto;

public record MyProfileResponse(
        String id,
        String username,
        String email,
        String phone,
        String firstName,
        String lastName,
        boolean notifyEmail,
        boolean notifySms
) {
}

