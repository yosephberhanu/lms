package et.yoseph.lms.notification.web.dto;

import java.time.Instant;
import java.util.UUID;

public record InAppNotificationResponse(UUID id, String title, String body, Instant createdAt, Instant readAt) {
}
