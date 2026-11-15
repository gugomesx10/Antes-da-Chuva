#!/usr/bin/env bash
# Antes da Chuva - build do jar e deploy manual no Web App.
# Útil antes da Release do Azure DevOps estar pronta. Exige ./scripts/azure-setup.sh já executado.
#
# Uso: ./scripts/azure-deploy.sh

set -euo pipefail
source "$(dirname "${BASH_SOURCE[0]}")/azure-common.sh"

log "Verificando Azure CLI e login"
require_az_login

log "Build do jar (testes são executados no CI)"
(cd "$ROOT_DIR" && sh ./mvnw -B -q clean package -DskipTests)
JAR="$(ls "$ROOT_DIR"/target/*.jar | grep -v -- '-plain.jar' | head -n 1)"
ok "$JAR"

log "Deploy no Web App $WEBAPP_NAME"
az webapp deploy \
    --resource-group "$RESOURCE_GROUP" \
    --name "$WEBAPP_NAME" \
    --src-path "$JAR" \
    --type jar \
    --output none

APP_URL="https://$(az webapp show --resource-group "$RESOURCE_GROUP" --name "$WEBAPP_NAME" --query defaultHostName -o tsv)"

log "Aguardando a aplicação responder em $APP_URL/actuator/health"
for attempt in $(seq 1 30); do
    if curl -fsS "$APP_URL/actuator/health" | grep -q '"UP"'; then
        ok "Aplicação no ar: $APP_URL/swagger-ui/index.html"
        exit 0
    fi
    sleep 10
done

fail "A aplicação não respondeu em 5 minutos. Veja os logs: az webapp log tail -g $RESOURCE_GROUP -n $WEBAPP_NAME"
