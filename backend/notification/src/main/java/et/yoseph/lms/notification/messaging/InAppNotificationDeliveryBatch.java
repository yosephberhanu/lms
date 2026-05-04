package et.yoseph.lms.notification.messaging;

import et.yoseph.lms.notification.service.NotificationPush;

import java.util.List;

/**
 * AMQP payload: fan-out to SSE happens asynchronously after DB commit.
 */
public record InAppNotificationDeliveryBatch(List<NotificationPush> pushes) {
}
