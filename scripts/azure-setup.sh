#!/usr/bin/env bash
# Antes da Chuva - criação da infraestrutura Azure.
#
# Uso:
#   cp scripts/.env.example scripts/.env   # preencha os valores
#   az login
#   ./scripts/azure-setup.sh
#
# Pode ser executado mais de uma vez: recursos já existentes são mantidos
# e as App Settings são reaplicadas.

set -euo pipefail
source "$(dirname "${BASH_SOURCE[0]}")/azure-common.sh"

: "${DB_ADMIN_USER:=adminchuva}"
: "${DB_ADMIN_PASSWORD:?Defina DB_ADMIN_PASSWORD}"
: "${RABBITMQ_USERNAME:=chuva}"
: "${RABBITMQ_PASSWORD:?Defina RABBITMQ_PASSWORD}"
: "${JWT_SECRET:?Defina JWT_SECRET (openssl rand -base64 48)}"
: "${GEMINI_API_KEY:?Defina GEMINI_API_KEY}"
: "${AI_MODEL:=gemini-2.5-flash}"
: "${ALLOW_MY_IP:=true}"

log "Verificando Azure CLI e login"
require_az_login

# ---------------------------------------------------------------------------
log "Registrando provedores de recursos (necessário em assinaturas novas)"
for provider in Microsoft.DBforPostgreSQL Microsoft.ContainerRegistry Microsoft.ContainerInstance Microsoft.Web; do
    state="$(az provider show --namespace "$provider" --query registrationState -o tsv 2>/dev/null || echo NotRegistered)"
    if [[ "$state" != "Registered" ]]; then
        warn "Registrando $provider (pode levar alguns minutos)..."
        az provider register --namespace "$provider" --wait --output none
    fi
    ok "$provider: Registered"
done

# ---------------------------------------------------------------------------
log "1/6 Resource Group: $RESOURCE_GROUP ($LOCATION)"
az group create --name "$RESOURCE_GROUP" --location "$LOCATION" --output none
ok "ok"

# ---------------------------------------------------------------------------
log "2/6 PostgreSQL Flexible Server: $PG_SERVER"
# Lista em vez de "show": se a consulta falhar, o script para (set -e) em vez de
# concluir que o servidor não existe e tentar recriá-lo.
EXISTING_PG="$(az postgres flexible-server list --resource-group "$RESOURCE_GROUP" --query "[?name=='$PG_SERVER'].state" -o tsv)"
if [[ -n "$EXISTING_PG" ]]; then
    ok "Servidor já existe (estado: $EXISTING_PG), mantendo"
else
    # --public-access 0.0.0.0 libera apenas serviços do próprio Azure (o Web App)
    az postgres flexible-server create \
        --resource-group "$RESOURCE_GROUP" \
        --name "$PG_SERVER" \
        --location "$LOCATION" \
        --admin-user "$DB_ADMIN_USER" \
        --admin-password "$DB_ADMIN_PASSWORD" \
        --tier Burstable \
        --sku-name Standard_B1ms \
        --storage-size 32 \
        --version 16 \
        --public-access 0.0.0.0 \
        --yes \
        --output none
    ok "Servidor criado"
fi

if ! az postgres flexible-server db show --resource-group "$RESOURCE_GROUP" --server-name "$PG_SERVER" --name "$PG_DATABASE" --output none 2>/dev/null; then
    az postgres flexible-server db create \
        --resource-group "$RESOURCE_GROUP" \
        --server-name "$PG_SERVER" \
        --name "$PG_DATABASE" \
        --output none
    ok "Banco $PG_DATABASE criado"
fi

if [[ "$ALLOW_MY_IP" == "true" ]]; then
    MY_IP="$(curl -fsS https://api.ipify.org || true)"
    if [[ -n "$MY_IP" ]]; then
        az postgres flexible-server firewall-rule create \
            --resource-group "$RESOURCE_GROUP" \
            --server-name "$PG_SERVER" \
            --name "admin-local" \
            --start-ip-address "$MY_IP" \
            --end-ip-address "$MY_IP" \
            --output none
        ok "IP $MY_IP liberado no firewall (para psql/DBeaver)"
    else
        warn "Não foi possível descobrir o IP público; firewall local não configurado"
    fi
fi

PG_HOST="$(az postgres flexible-server show --resource-group "$RESOURCE_GROUP" --name "$PG_SERVER" --query fullyQualifiedDomainName -o tsv)"
DB_URL="jdbc:postgresql://${PG_HOST}:5432/${PG_DATABASE}?sslmode=require"

# ---------------------------------------------------------------------------
log "3/6 Container Registry e imagem do RabbitMQ: $ACR_NAME ($ACR_LOCATION)"
CURRENT_ACR_LOCATION="$(az acr show --resource-group "$RESOURCE_GROUP" --name "$ACR_NAME" --query location -o tsv 2>/dev/null || true)"

# O registry só guarda a imagem que este script gera, então recriá-lo em outra região é seguro.
if [[ -n "$CURRENT_ACR_LOCATION" && "$CURRENT_ACR_LOCATION" != "$ACR_LOCATION" ]]; then
    warn "Registry está em $CURRENT_ACR_LOCATION; recriando em $ACR_LOCATION (região com ACR Tasks)"
    az acr delete --resource-group "$RESOURCE_GROUP" --name "$ACR_NAME" --yes --output none
    CURRENT_ACR_LOCATION=""
fi

if [[ -z "$CURRENT_ACR_LOCATION" ]]; then
    az acr create \
        --resource-group "$RESOURCE_GROUP" \
        --name "$ACR_NAME" \
        --location "$ACR_LOCATION" \
        --sku Basic \
        --admin-enabled true \
        --output none
    ok "Registry criado"
    # O registry recém-criado leva alguns segundos para aceitar operações
    sleep 20
else
    ok "Registry já existe"
fi

ACR_LOGIN_SERVER="$(az acr show --name "$ACR_NAME" --query loginServer -o tsv)"
ACR_USER="$(az acr credential show --name "$ACR_NAME" --query username -o tsv)"
ACR_PASSWORD="$(az acr credential show --name "$ACR_NAME" --query 'passwords[0].value' -o tsv)"

docker_usable() {
    command -v docker >/dev/null 2>&1 && docker info >/dev/null 2>&1
}

# 1ª opção: build na nuvem (ACR Tasks). O erro é guardado para explicar a falha.
ACR_BUILD_LOG="$(mktemp)"
if az acr build \
        --registry "$ACR_NAME" \
        --image "$RABBIT_IMAGE" \
        --file "$ROOT_DIR/dockerfiles/rabbitmq/Dockerfile" \
        "$ROOT_DIR/dockerfiles/rabbitmq" \
        --output none >"$ACR_BUILD_LOG" 2>&1; then
    ok "Imagem $RABBIT_IMAGE construída com ACR Tasks"
else
    warn "ACR Tasks falhou. Motivo:"
    grep -E 'ERROR|Code:|Message:' "$ACR_BUILD_LOG" | head -n 3 | sed 's/^/      /' >&2 || true

    # 2ª opção: Docker local
    if docker_usable; then
        warn "Usando Docker local"
        az acr login --name "$ACR_NAME"
        docker build -t "$ACR_LOGIN_SERVER/$RABBIT_IMAGE" "$ROOT_DIR/dockerfiles/rabbitmq"
        docker push "$ACR_LOGIN_SERVER/$RABBIT_IMAGE"
        ok "Imagem $RABBIT_IMAGE enviada via Docker local"
    else
        rm -f "$ACR_BUILD_LOG"
        fail "Sem ACR Tasks e sem acesso ao Docker local.
       Se o erro for 'permission denied ... docker.sock', libere o Docker para seu usuário:
         sudo usermod -aG docker \$USER   # depois saia e entre de novo na sessão (ou: newgrp docker)
         docker info                      # deve funcionar sem sudo
       Depois rode este script novamente."
    fi
fi
rm -f "$ACR_BUILD_LOG"

# ---------------------------------------------------------------------------
log "4/6 Container Instance com RabbitMQ: $RABBIT_CONTAINER"
if az container show --resource-group "$RESOURCE_GROUP" --name "$RABBIT_CONTAINER" --output none 2>/dev/null; then
    ok "Container já existe, mantendo"
else
    az container create \
        --resource-group "$RESOURCE_GROUP" \
        --name "$RABBIT_CONTAINER" \
        --location "$LOCATION" \
        --image "$ACR_LOGIN_SERVER/$RABBIT_IMAGE" \
        --registry-login-server "$ACR_LOGIN_SERVER" \
        --registry-username "$ACR_USER" \
        --registry-password "$ACR_PASSWORD" \
        --os-type Linux \
        --cpu 1 \
        --memory 1.5 \
        --ports 5672 15672 \
        --ip-address Public \
        --dns-name-label "$RABBIT_DNS_LABEL" \
        --restart-policy Always \
        --environment-variables RABBITMQ_DEFAULT_USER="$RABBITMQ_USERNAME" \
        --secure-environment-variables RABBITMQ_DEFAULT_PASS="$RABBITMQ_PASSWORD" \
        --output none
    ok "Container criado"
fi

RABBIT_HOST="$(az container show --resource-group "$RESOURCE_GROUP" --name "$RABBIT_CONTAINER" --query ipAddress.fqdn -o tsv)"

# ---------------------------------------------------------------------------
log "5/6 App Service Plan + Web App Java 21: $WEBAPP_NAME"
if ! az appservice plan show --resource-group "$RESOURCE_GROUP" --name "$APP_PLAN" --output none 2>/dev/null; then
    az appservice plan create \
        --resource-group "$RESOURCE_GROUP" \
        --name "$APP_PLAN" \
        --location "$LOCATION" \
        --is-linux \
        --sku B1 \
        --output none
    ok "Plano B1 Linux criado"
else
    ok "Plano já existe"
fi

if ! az webapp show --resource-group "$RESOURCE_GROUP" --name "$WEBAPP_NAME" --output none 2>/dev/null; then
    az webapp create \
        --resource-group "$RESOURCE_GROUP" \
        --plan "$APP_PLAN" \
        --name "$WEBAPP_NAME" \
        --runtime "JAVA|21-java21" \
        --output none
    ok "Web App criado"
else
    ok "Web App já existe"
fi

az webapp config set \
    --resource-group "$RESOURCE_GROUP" \
    --name "$WEBAPP_NAME" \
    --always-on true \
    --generic-configurations '{"healthCheckPath": "/actuator/health"}' \
    --output none

az webapp log config \
    --resource-group "$RESOURCE_GROUP" \
    --name "$WEBAPP_NAME" \
    --application-logging filesystem \
    --level information \
    --output none

# ---------------------------------------------------------------------------
log "6/6 App Settings do Web App (valores não são exibidos)"
az webapp config appsettings set \
    --resource-group "$RESOURCE_GROUP" \
    --name "$WEBAPP_NAME" \
    --settings \
        PORT=8080 \
        WEBSITES_PORT=8080 \
        DB_URL="$DB_URL" \
        DB_USERNAME="$DB_ADMIN_USER" \
        DB_PASSWORD="$DB_ADMIN_PASSWORD" \
        RABBITMQ_HOST="$RABBIT_HOST" \
        RABBITMQ_PORT=5672 \
        RABBITMQ_USERNAME="$RABBITMQ_USERNAME" \
        RABBITMQ_PASSWORD="$RABBITMQ_PASSWORD" \
        RABBITMQ_SSL_ENABLED=false \
        JWT_SECRET="$JWT_SECRET" \
        JWT_ISSUER="https://${WEBAPP_NAME}.azurewebsites.net" \
        GEMINI_API_KEY="$GEMINI_API_KEY" \
        AI_MODEL="$AI_MODEL" \
    --output none
ok "App Settings aplicadas"

# ---------------------------------------------------------------------------
APP_URL="https://$(az webapp show --resource-group "$RESOURCE_GROUP" --name "$WEBAPP_NAME" --query defaultHostName -o tsv)"

log "Infraestrutura pronta"
cat <<EOF

  Resource Group   $RESOURCE_GROUP
  PostgreSQL       $PG_HOST  (banco: $PG_DATABASE)
  RabbitMQ AMQP    $RABBIT_HOST:5672
  RabbitMQ painel  http://$RABBIT_HOST:15672
  Web App          $APP_URL
  Swagger          $APP_URL/swagger-ui/index.html
  Health           $APP_URL/actuator/health

  Próximo passo: publicar o jar
    ./scripts/azure-deploy.sh      (manual)
    ou a Release do Azure DevOps   (automático)

EOF
