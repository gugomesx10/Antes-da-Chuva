package br.com.fiap.gs.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableRabbit
@EnableScheduling
public class RabbitMQConfig {

    @Bean
    public DirectExchange incidentExchange(
            @Value("${app.rabbitmq.exchange}") String name
    ) {
        return new DirectExchange(name, true, false);
    }

    @Bean
    public Queue incidentTriageQueue(
            @Value("${app.rabbitmq.queue}") String name
    ) {
        return QueueBuilder.durable(name).build();
    }

    @Bean
    public Binding incidentBinding(
            Queue incidentTriageQueue,
            DirectExchange incidentExchange,
            @Value("${app.rabbitmq.routing-key}") String routingKey
    ) {
        return BindingBuilder
                .bind(incidentTriageQueue)
                .to(incidentExchange)
                .with(routingKey);
    }
}
