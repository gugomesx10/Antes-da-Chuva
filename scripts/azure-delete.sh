#!/usr/bin/env bash
# Antes da Chuva - remove TODOS os recursos criados por azure-setup.sh.
#
# Uso: ./scripts/azure-delete.sh          (pede confirmação)
#      ./scripts/azure-delete.sh --yes    (sem confirmação)

set -euo pipefail
source "$(dirname "${BASH_SOURCE[0]}")/azure-common.sh"

log "Verificando Azure CLI e login"
require_az_login

if ! az group show --name "$RESOURCE_GROUP" --output none 2>/dev/null; then
    ok "Resource Group $RESOURCE_GROUP não existe; nada a remover"
    exit 0
fi

log "Recursos em $RESOURCE_GROUP"
az resource list --resource-group "$RESOURCE_GROUP" --query '[].{nome:name, tipo:type}' -o table

if [[ "${1:-}" != "--yes" ]]; then
    printf '\nDigite o nome do Resource Group para confirmar a exclusão (%s): ' "$RESOURCE_GROUP"
    read -r answer
    [[ "$answer" == "$RESOURCE_GROUP" ]] || fail "Confirmação não confere; nada foi removido"
fi

log "Removendo $RESOURCE_GROUP (em segundo plano no Azure)"
az group delete --name "$RESOURCE_GROUP" --yes --no-wait
ok "Exclusão iniciada. Acompanhe com: az group show -n $RESOURCE_GROUP"
