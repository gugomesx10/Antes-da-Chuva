package br.com.fiap.gs.messaging;

import br.com.fiap.gs.service.IncidentTriageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class IncidentTriageConsumer {

    private final IncidentTriageService triageService;

    @RabbitListener(queues = "${app.rabbitmq.queue}")
    public void consume(String message) {
        Long incidentId = Long.valueOf(message);

        log.info(
                "Recebida solicitação de triagem da ocorrência {}",
                incidentId
        );

        triageService.process(incidentId);
    }
}