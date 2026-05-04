package et.yoseph.lms.notification.web.dto;

public record UserCommunicationPreferenceUpdateRequest(
        boolean notifyEmail,
        boolean notifySms,
        String email,
        String phone
) {
}
