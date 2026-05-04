package et.yoseph.lms.notification.service;

import et.yoseph.lms.notification.messaging.InAppNotificationDeliveryBatch;
import et.yoseph.lms.notification.messaging.InAppNotificationRabbitProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class NotificationDispatchService {

    private final TaskExecutor notificationDispatchExecutor;
    private final NotificationSseRegistry sseRegistry;
    private final ExternalNotificationDeliveryService externalDeliveryService;
    private final String broker;
    private final ObjectProvider<RabbitTemplate> rabbitTemplate;
    private final ObjectProvider<InAppNotificationRabbitProperties> rabbitProperties;

    public NotificationDispatchService(
            @Qualifier("notificationDispatchExecutor") TaskExecutor notificationDispatchExecutor,
            NotificationSseRegistry sseRegistry,
            ExternalNotificationDeliveryService externalDeliveryService,
            @Value("${lms.notification.broker:local}") String broker,
            ObjectProvider<RabbitTemplate> rabbitTemplate,
            ObjectProvider<InAppNotificationRabbitProperties> rabbitProperties) {
        this.notificationDispatchExecutor = notificationDispatchExecutor;
        this.sseRegistry = sseRegistry;
        this.externalDeliveryService = externalDeliveryService;
        this.broker = broker;
        this.rabbitTemplate = rabbitTemplate;
        this.rabbitProperties = rabbitProperties;
    }

    /**
     * After DB commit: publish to RabbitMQ or deliver on a worker thread (local mode).
     */
    public void enqueueAfterCommit(List<NotificationPush> pushes) {
        if (pushes.isEmpty()) {
            return;
        }
        List<NotificationPush> snapshot = List.copyOf(pushes);
        if ("rabbitmq".equalsIgnoreCase(broker)) {
            RabbitTemplate template = rabbitTemplate.getIfAvailable();
            InAppNotificationRabbitProperties props = rabbitProperties.getIfAvailable();
            if (template == null || props == null) {
                throw new IllegalStateException(
                        "lms.notification.broker=rabbitmq but RabbitMQ is not configured (missing RabbitTemplate).");
            }
            template.convertAndSend(
                    props.getExchange(),
                    props.getRoutingKey(),
                    new InAppNotificationDeliveryBatch(snapshot));
            return;
        }
        notificationDispatchExecutor.execute(() -> deliverSync(snapshot));
    }

    public void deliverSync(List<NotificationPush> pushes) {
        for (NotificationPush p : pushes) {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("id", p.id().toString());
            payload.put("title", p.title());
            payload.put("body", p.body());
            payload.put("createdAt", p.createdAtEpochMs());
            sseRegistry.sendPayload(p.userSub(), payload);
            externalDeliveryService.deliver(p);
        }
    }
}
