package et.yoseph.lms.notification.service;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Sends email/SMS copies of in-app notifications when enabled.
 */
@Service
public class ExternalNotificationDeliveryService {
    private final UserContactPreferencesResolver resolver;
    private final ExternalMessagingService messagingService;
    public ExternalNotificationDeliveryService(UserContactPreferencesResolver resolver, ExternalMessagingService messagingService) {
        this.resolver = resolver;
        this.messagingService = messagingService;
    }

    public void deliver(NotificationPush push) {
        resolver.resolveByUserSub(push.userSub()).ifPresent(user -> {
            boolean email = user.notifyEmail() && StringUtils.hasText(user.email());
            boolean sms = user.notifySms() && StringUtils.hasText(user.phone());
            if (email) {
                messagingService.sendEmail(user.email(), push.title(), push.body());
            }
            if (sms) {
                messagingService.sendSMS(user.phone(), push.title() + ": " + push.body());
            }
        });
    }
}
