package et.yoseph.lms.notification.web.dto;

public record UserCommunicationPreferenceResponse(
        boolean notifyEmail,
        boolean notifySms,
        String email,
        String phone
) {
}
