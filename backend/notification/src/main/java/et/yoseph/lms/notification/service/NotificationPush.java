package et.yoseph.lms.notification.service;

import java.util.UUID;

public record NotificationPush(String userSub, UUID id, String title, String body, long createdAtEpochMs) {
}
