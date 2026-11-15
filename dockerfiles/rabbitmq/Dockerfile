# RabbitMQ usado pela triagem assíncrona (exchange incident.exchange, fila incident.triage.queue).
# A exchange, a fila e o binding são declarados pela própria aplicação (RabbitMQConfig).
# Usuário e senha chegam por variável de ambiente (RABBITMQ_DEFAULT_USER / RABBITMQ_DEFAULT_PASS).
FROM rabbitmq:3.13-management

COPY rabbitmq.conf /etc/rabbitmq/conf.d/20-antes-da-chuva.conf

# 5672 = AMQP (aplicação) | 15672 = painel de gerenciamento
EXPOSE 5672 15672

HEALTHCHECK --interval=30s --timeout=10s --retries=5 \
    CMD rabbitmq-diagnostics -q ping || exit 1
