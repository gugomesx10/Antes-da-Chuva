#!/usr/bin/env bash
# Configuração compartilhada pelos scripts azure-*.sh. Não execute diretamente.

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

# Carrega scripts/.env quando existir (localmente). No pipeline as variáveis já vêm do ambiente.
if [[ -f "$SCRIPT_DIR/.env" ]]; then
    set -a
    # shellcheck disable=SC1091
    source "$SCRIPT_DIR/.env"
    set +a
fi

: "${RM:?Defina RM no scripts/.env (ex.: RM=554321)}"
LOCATION="${LOCATION:-brazilsouth}"
# O ACR Tasks não existe em todas as regiões (ex.: chilecentral não tem).
# O registry pode ficar em outra região já que o container baixa a imagem de qualquer lugar.
ACR_LOCATION="${ACR_LOCATION:-brazilsouth}"

# Nomes dos recursos. Os que viram DNS público levam o RM para serem únicos.
RESOURCE_GROUP="rg-antesdachuva"
PG_SERVER="pg-antesdachuva-${RM}"
PG_DATABASE="antesdachuva"
ACR_NAME="acrantesdachuva${RM}"
RABBIT_IMAGE="antes-da-chuva-rabbitmq:1.0"
RABBIT_CONTAINER="aci-rabbitmq-antesdachuva"
RABBIT_DNS_LABEL="rabbit-antesdachuva-${RM}"
APP_PLAN="plan-antesdachuva"
WEBAPP_NAME="app-antesdachuva-${RM}"

log()  { printf '\n\033[1;34m==> %s\033[0m\n' "$*"; }
ok()   { printf '    \033[0;32m%s\033[0m\n' "$*"; }
warn() { printf '    \033[0;33m%s\033[0m\n' "$*"; }
fail() { printf '\033[0;31mERRO: %s\033[0m\n' "$*" >&2; exit 1; }

require_az_login() {
    command -v az >/dev/null 2>&1 || fail "Azure CLI não encontrado. Instale: https://learn.microsoft.com/cli/azure/install-azure-cli"
    az account show --output none 2>/dev/null || fail "Faça login antes: az login"
    ok "Assinatura: $(az account show --query name -o tsv)"
}
