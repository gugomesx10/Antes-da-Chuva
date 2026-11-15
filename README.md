# Antes da Chuva

**Plataforma de zeladoria preventiva com triagem assistida por inteligência artificial.**

Projeto da **Global Solution FIAP — 2026/2**. Cidadãos registram problemas urbanos associados a drenagem e alagamentos; gestores acompanham o atendimento e atualizam o andamento. A classificação pela IA é uma **sugestão**, não um alerta oficial da Defesa Civil.

## O problema e a solução

Ocorrências como bueiros obstruídos, entulho em vias públicas e pontos de alagamento podem ser difíceis de acompanhar quando os relatos são descentralizados. O Antes da Chuva centraliza esses registros, mantém o histórico de atendimento e usa IA para sugerir urgência e orientações preventivas.

## Arquitetura

```mermaid
flowchart TD
    A[Aplicativo móvel / cliente REST] -->|JWT| B[API Spring Boot]
    B --> C[(PostgreSQL)]
    B -->|Após commit| D[RabbitMQ]
    D --> E[Consumidor de triagem]
    E --> F[Recuperação RAG lexical]
    F --> G[Spring AI + Gemini]
    G --> E
    E --> C
    H[Gestor] -->|Atualização de status| B
```

**Fluxo:** o cidadão autentica-se, registra uma ocorrência e recebe um identificador. O Java persiste o registro com `OPEN` e `PENDING`, publica seu ID na fila após a transação e o consumidor solicita a análise da IA. O resultado é persistido em `urgency`, `aiSummary`, `aiGuidance` e `triageStatus`. O gestor pode registrar mudanças de status com histórico.

## Stack

| Camada | Tecnologia |
| --- | --- |
| Linguagem | Java 21 |
| API | Spring Boot 4.1.2 / Spring Web MVC |
| Autenticação | Spring Security, JWT HS256 e BCrypt |
| Persistência | Spring Data JPA, PostgreSQL e Flyway |
| Assíncrono | Spring AMQP / RabbitMQ |
| IA | Spring AI 2.0.2 / Google Gemini |
| RAG | Recuperação lexical em documentos de prevenção |
| Documentação | OpenAPI / Swagger UI |
| Testes | JUnit 5, Mockito e MockMvc |
| Infraestrutura | Azure (deploy planejado) |

## Perfis e domínio

- `CIDADAO`: registra, consulta e, quando permitido, edita ou cancela ocorrências próprias.
- `GESTOR`: gerencia categorias e atualiza o status de ocorrências.
- O cadastro público cria apenas `CIDADAO`; o primeiro `GESTOR` deve ser provisionado por processo administrativo seguro.

Entidades principais: `User`, `Category`, `Incident`, `IncidentUpdate`; a modelagem inclui `Endorsement` e `IncidentAudit`.

Status: `OPEN`, `IN_REVIEW`, `IN_PROGRESS`, `RESOLVED`, `REJECTED`, `CANCELLED`.

Urgência: `LOW`, `MEDIUM`, `HIGH`, `CRITICAL`. Triagem: `PENDING`, `DONE`, `FAILED`.

## Endpoints

| Método | Rota | Objetivo | Acesso |
| --- | --- | --- | --- |
| POST | `/auth/register` | Cadastro | Público |
| POST | `/auth/login` | Login / JWT | Público |
| GET | `/auth/me` | Usuário atual | Autenticado |
| GET | `/categories` | Lista paginada | Autenticado |
| GET | `/categories/{id}` | Detalhe | Autenticado |
| POST | `/categories` | Criar categoria | GESTOR |
| PUT | `/categories/{id}` | Atualizar categoria | GESTOR |
| DELETE | `/categories/{id}` | Desativar categoria | GESTOR |
| GET | `/incidents` | Listagem com filtros | Autenticado |
| GET | `/incidents/mine` | Minhas ocorrências | CIDADAO |
| GET | `/incidents/{id}` | Detalhe | Autenticado |
| POST | `/incidents` | Registrar ocorrência | CIDADAO |
| PUT | `/incidents/{id}` | Editar ocorrência própria | CIDADAO |
| DELETE | `/incidents/{id}` | Cancelar ocorrência própria | CIDADAO |
| GET | `/incidents/{incidentId}/updates` | Histórico de status | Autenticado |
| POST | `/incidents/{incidentId}/updates` | Alterar status | GESTOR |

`GET /incidents` aceita `status`, `categoryId` e `neighborhood` como filtros opcionais. Endpoints de listagem oferecem paginação.

**Swagger:** `/swagger-ui/index.html` • **Saúde:** `/actuator/health`.

### Exemplo de cadastro

```json
{
  "name": "Pessoa Exemplo",
  "email": "pessoa.exemplo@example.com",
  "password": "SenhaExemplo2026!",
  "neighborhood": "Mooca"
}
```

Envie para `POST /auth/register`.

### Exemplo de ocorrência

```json
{
  "categoryId": 1,
  "title": "Bueiro obstruído por lixo",
  "description": "Há acúmulo de água na rua após as chuvas.",
  "address": "Rua Exemplo, 100",
  "neighborhood": "Mooca",
  "latitude": -23.550000,
  "longitude": -46.630000
}
```

Envie para `POST /incidents` com `Authorization: Bearer <TOKEN_JWT>`. A categoria `1` é **Bueiro entupido**, criada pela migration `V2__seed_categories.sql`.

## RAG e IA

Os documentos em `src/main/resources/knowledge/` abrangem bueiros, alagamentos e prevenção. O `RagKnowledgeService` normaliza palavras e seleciona até dois documentos relacionados ao relato. O `IncidentAiService` passa esses trechos como contexto para o Gemini e recebe um `AiTriageResult` com urgência, resumo e orientação.

**Limite técnico:** o RAG é lexical, sem embeddings nem banco vetorial. Não há previsão meteorológica em tempo real nem validação oficial dos relatos.

## Configuração para execução

Requer **Java 21**, acesso a **PostgreSQL**, **RabbitMQ** e **Gemini API**. Os serviços podem ser remotos. Configure as variáveis de ambiente no host de execução, nunca no repositório:

```dotenv
DB_URL=jdbc:postgresql://<servidor>:5432/<banco>?sslmode=require
DB_USERNAME=<usuario>
DB_PASSWORD=<senha>
RABBITMQ_HOST=<servidor>
RABBITMQ_PORT=5672
RABBITMQ_USERNAME=<usuario>
RABBITMQ_PASSWORD=<senha>
RABBITMQ_SSL_ENABLED=false
GEMINI_API_KEY=<chave>
AI_MODEL=gemini-2.5-flash
JWT_SECRET=<segredo-base64-de-pelo-menos-32-bytes>
JWT_ISSUER=https://antes-da-chuva.example
JWT_EXPIRATION_SECONDS=3600
PORT=8080
```

Se o RabbitMQ exigir TLS, ajuste `RABBITMQ_SSL_ENABLED=true` e a porta do serviço (frequentemente `5671`). As tabelas e categorias iniciais são geradas pelas migrations Flyway em `src/main/resources/db/migration`.

### Build e inicialização

```bash
mvnd clean package -DskipTests
java -jar target/Global-Solution-Antes-Da-Chuva-0.0.1-SNAPSHOT.jar
```

O projeto também possui Maven Wrapper (`./mvnw`). O JAR depende dos serviços e segredos configurados para inicializar corretamente.

## Testes e estado atual

Foram aprovados testes unitários de cadastro, login, assinatura JWT, mapeamento de perfis, autorização HTTP, recuperação RAG, triagem e reenvio de mensagens pendentes. Também foi executado um teste de integração **real** com Spring AI + Gemini + RAG: um relato fictício de bueiro entupido recebeu a urgência `HIGH`, com resumo e orientação preventiva.

```bash
mvnd -Dtest=AuthServiceTest,JwtTokenServiceTest,JwtRoleMappingTest,CategorySecurityTest test
mvnd -Dtest=RagKnowledgeServiceTest,IncidentTriageServiceTest,PendingTriageRetryJobTest test
mvnd -Dtest=IncidentAiIntegrationIT test
```

O último teste faz chamada externa e exige `GEMINI_API_KEY` válida. Não inclui persistência ou RabbitMQ.

**Ainda pendente:** validar o fluxo completo API → PostgreSQL → RabbitMQ → Gemini → PostgreSQL em ambiente Azure, configurar o primeiro GESTOR e reforçar idempotência/reenvio de mensagens. O agendamento atual tenta reenviar ocorrências pendentes antigas, mas não oferece uma garantia transacional completa de entrega.

## Organização

```text
src/main/java/br/com/fiap/gs/
├── ai/
├── config/
├── controller/
├── dto/
├── entity/
├── enums/
├── exception/
├── mapper/
├── messaging/
├── repository/
├── security/
└── service/
src/main/resources/
├── application.yml
├── db/migration/
└── knowledge/
```

---

**FIAP — Global Solution 2026/2 | Antes da Chuva**
