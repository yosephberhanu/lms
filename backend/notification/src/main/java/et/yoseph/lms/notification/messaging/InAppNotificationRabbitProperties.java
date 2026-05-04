package et.yoseph.lms.notification.messaging;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "lms.notification.rabbit")
public class InAppNotificationRabbitProperties {

    /**
     * Topic exchange for LMS notification events.
     */
    private String exchange = "lms.notification";

    /**
     * Durable queue consumed by this service to deliver in-app rows to SSE subscribers.
     */
    private String queue = "lms.notification.in-app";

    /**
     * Routing key for {@link InAppNotificationDeliveryBatch} messages.
     */
    private String routingKey = "in-app.notification.delivered";

    public String getExchange() {
        return exchange;
    }

    public void setExchange(String exchange) {
        this.exchange = exchange;
    }

    public String getQueue() {
        return queue;
    }

    public void setQueue(String queue) {
        this.queue = queue;
    }

    public String getRoutingKey() {
        return routingKey;
    }

    public void setRoutingKey(String routingKey) {
        this.routingKey = routingKey;
    }
}
