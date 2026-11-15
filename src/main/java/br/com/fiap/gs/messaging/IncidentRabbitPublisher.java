package br.com.fiap.gs.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class IncidentRabbitPublisher {

    private final RabbitTemplate rabbitTemplate;

    @Value("${app.rabbitmq.exchange}")
    private String exchange;

    @Value("${app.rabbitmq.routing-key}")
    private String routingKey;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publish(IncidentTriageRequested event) {
        try {
            rabbitTemplate.convertAndSend(
                    exchange,
                    routingKey,
                    event.incidentId().toString()
            );

            log.info(
                    "Ocorrência {} enviada para triagem",
                    event.incidentId()
            );
        } catch (AmqpException ex) {
            log.error(
                    "Falha ao publicar ocorrência {}",
                    event.incidentId(),
                    ex
            );
        }
    }
}
