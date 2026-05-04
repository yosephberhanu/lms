package et.yoseph.lms.notification.messaging;

import et.yoseph.lms.notification.service.NotificationDispatchService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "lms.notification.broker", havingValue = "rabbitmq")
public class InAppNotificationDeliveryListener {

    private final NotificationDispatchService dispatchService;

    public InAppNotificationDeliveryListener(NotificationDispatchService dispatchService) {
        this.dispatchService = dispatchService;
    }

    @RabbitListener(queues = "${lms.notification.rabbit.queue}")
    public void onDeliver(InAppNotificationDeliveryBatch batch) {
        if (batch == null || batch.pushes() == null || batch.pushes().isEmpty()) {
            return;
        }
        dispatchService.deliverSync(batch.pushes());
    }
}
