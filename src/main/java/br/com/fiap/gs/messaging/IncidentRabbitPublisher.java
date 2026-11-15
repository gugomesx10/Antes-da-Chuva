package br.com.fiap.gs.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class IncidentRabbitPublisher {

    private final RabbitTemplate rabbitTemplate;

    @Value("${app.rabbitmq.exchange}")
    private String exchange;

    @Value("${app.rabbitmq.routing-key}")
    private String routingKey;

    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT
    )
    public void publish(IncidentTriageRequested event) {
        send(event.incidentId());
    }

    public void retry(Long incidentId) {
        send(incidentId);
    }

    private void send(Long incidentId) {
        CorrelationData correlation =
                new CorrelationData(UUID.randomUUID().toString());

        try {
            rabbitTemplate.convertAndSend(
                    exchange,
                    routingKey,
                    incidentId.toString(),
                    correlation
            );

            correlation.getFuture().whenComplete((confirm, error) -> {
                if (error != null
                        || confirm == null
                        || !confirm.ack()
                        || correlation.getReturned() != null) {

                    log.warn(
                            "Publicação da ocorrência {} não confirmada",
                            incidentId,
                            error
                    );

                    return;
                }

                log.info(
                        "RabbitMQ confirmou a ocorrência {}",
                        incidentId
                );
            });

        } catch (AmqpException ex) {
            log.error(
                    "RabbitMQ indisponível para ocorrência {}",
                    incidentId,
                    ex
            );
        }
    }
}