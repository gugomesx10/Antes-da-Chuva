# Contrato Java verificado

Fonte: https://github.com/gugomesx10/Antes-da-Chuva, commit 1e0b7ad19cb1763bfaed167864cb5123ef5315af.

- POST /auth/register: name, email, password (8–72), neighborhood; retorna accessToken, tokenType, expiresIn, user.
- POST /auth/login: email, password; mesma resposta.
- GET /auth/me: usuário atual.
- GET /categories: página Spring com categorias reais (id, name, active).
- GET /incidents/mine (cidadão), GET /incidents (gestor): páginas Spring.
- GET /incidents/{id}: detalhes incluindo urgency, aiSummary, aiGuidance, triageStatus.
- POST /incidents e PUT /incidents/{id}: categoryId, title (120), description (1000), address (200), neighborhood (80), latitude/longitude opcionais.
- DELETE /incidents/{id}: 204; cancelamento lógico, somente dono e status OPEN. PUT também exige dono e OPEN.
- GET /incidents/{id}/updates: histórico; POST (gestor): newStatus, note obrigatória (500).

Enums: OPEN, IN_REVIEW, IN_PROGRESS, RESOLVED, REJECTED, CANCELLED; urgência LOW, MEDIUM, HIGH, CRITICAL. Não existe endpoint para editar prioridade: responsabilidade da IA.

Sem mocks ou fallback para ocorrências locais. Deploy precisa PostgreSQL, RabbitMQ, JWT_SECRET e GEMINI_API_KEY. O SecurityConfig libera CORS para as origens de `app.cors.allowed-origins` (variável `CORS_ALLOWED_ORIGINS`; padrão `http://localhost:8099,http://localhost:8081`). App nativo não usa CORS.
