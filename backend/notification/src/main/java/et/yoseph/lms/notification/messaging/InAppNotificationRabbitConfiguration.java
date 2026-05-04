package et.yoseph.lms.notification.messaging;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableRabbit
@EnableConfigurationProperties(InAppNotificationRabbitProperties.class)
@ConditionalOnProperty(name = "lms.notification.broker", havingValue = "rabbitmq")
@SuppressWarnings("removal")
public class InAppNotificationRabbitConfiguration {

    @Bean
    Jackson2JsonMessageConverter notificationJackson2JsonMessageConverter() {
        // Local ObjectMapper: avoids requiring a globally registered ObjectMapper bean (Boot 4 + this stack).
        return new Jackson2JsonMessageConverter(new ObjectMapper());
    }

    @Bean
    Declarables notificationTopology(InAppNotificationRabbitProperties props) {
        TopicExchange exchange = new TopicExchange(props.getExchange(), true, false);
        Queue queue = new Queue(props.getQueue(), true);
        Binding binding = BindingBuilder.bind(queue).to(exchange).with(props.getRoutingKey());
        return new Declarables(exchange, queue, binding);
    }

    @Bean
    ApplicationRunner notificationRabbitTemplateJsonSetup(
            RabbitTemplate rabbitTemplate,
            Jackson2JsonMessageConverter converter) {
        return args -> rabbitTemplate.setMessageConverter(converter);
    }
}
