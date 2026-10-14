#!/usr/bin/env bash
# Cloud Shell: criar/configurar Web App com banco e credenciais existentes.
set -euo pipefail

ROOT=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)
CONFIG="$ROOT/infra/azure.current.json"
JAR="$ROOT/target/dimdim-pedidos-0.1.0.jar"
EXECUTE=false
DEPLOY=false
while (($#)); do
  case "$1" in
    --execute) EXECUTE=true; shift ;;
    --deploy) DEPLOY=true; shift ;;
    --config) CONFIG="${2:?Informe o arquivo JSON}"; shift 2 ;;
    --jar) JAR="${2:?Informe o JAR}"; shift 2 ;;
    *) echo "Uso: bash infra/Deploy-ExistingAzure.sh [--config arquivo] [--execute] [--deploy] [--jar arquivo]" >&2; exit 1 ;;
  esac
done
command -v jq >/dev/null || { echo "jq é necessário (incluído no Cloud Shell)." >&2; exit 1; }
jq -e 'all(.subscriptionId,.resourceGroup,.location,.appServicePlan,.sourceWebApp,.webApp,.sqlServer,.database,.sqlAppUser; type == "string" and length > 0) and .appServiceSku == "F1" and .webApp != .sourceWebApp' "$CONFIG" >/dev/null
SUB=$(jq -r .subscriptionId "$CONFIG")
GROUP=$(jq -r .resourceGroup "$CONFIG")
LOCATION=$(jq -r .location "$CONFIG")
PLAN=$(jq -r .appServicePlan "$CONFIG")
SOURCE=$(jq -r .sourceWebApp "$CONFIG")
APP=$(jq -r .webApp "$CONFIG")
SQL=$(jq -r .sqlServer "$CONFIG")
DATABASE=$(jq -r .database "$CONFIG")
SQL_USER=$(jq -r .sqlAppUser "$CONFIG")
echo "Assinatura: $SUB"
echo "Destino: $GROUP / $APP; plano $PLAN (F1); origem das configurações: $SOURCE"
echo "Banco existente: $SQL / $DATABASE. Sem DDL ou redefinição de senhas."
echo "Publicar JAR: $DEPLOY ($JAR)"
if ! $EXECUTE; then
  echo "Somente plano. Use --execute para criar/configurar e --deploy para também publicar."
  exit 0
fi
if $DEPLOY && [[ ! -f "$JAR" ]]; then
  echo "Compile com Java 21 e bash ./mvnw verify antes de publicar." >&2
  exit 1
fi
command -v az >/dev/null
azc() { az "$@" --subscription "$SUB" --only-show-errors; }
# Confirmar a origem e o banco antes de alterar o destino.
azc webapp show -g "$GROUP" -n "$SOURCE" -o none
azc sql db show -g "$GROUP" -s "$SQL" -n "$DATABASE" -o none
umask 077
SETTINGS=$(mktemp)
trap 'rm -f -- "$SETTINGS"' EXIT
azc webapp config appsettings list -g "$GROUP" -n "$SOURCE" -o json |
  jq '[.[] | select(.name | test("^(DB_|APP_TIMEZONE$|JAVA_OPTS$|SPRING_PROFILES_ACTIVE$|APPLICATIONINSIGHTS_|APPINSIGHTS_|ApplicationInsights|XDT_MicrosoftApplicationInsights_)"))]' > "$SETTINGS"
jq -e --arg server "$SQL.database.windows.net" --arg database "databaseName=$DATABASE;" --arg user "$SQL_USER" '
  (map({key:.name,value:.value}) | from_entries) as $s |
  ($s.DB_URL | contains($server) and contains($database)) and
  ($s.DB_USERNAME == $user) and (($s.DB_PASSWORD // "") | length > 0) and
  (($s.SPRING_PROFILES_ACTIVE // "") | split(",") | map(gsub("^\\s+|\\s+$"; "")) | index("demo") == null)
' "$SETTINGS" >/dev/null || { echo "Configuração de banco/perfil da origem incompatível; execução interrompida." >&2; exit 1; }
PLAN_ID=$(azc appservice plan list -g "$GROUP" --query "[?name=='$PLAN'].id | [0]" -o tsv)
if [[ -z "$PLAN_ID" ]]; then
  azc appservice plan create -g "$GROUP" -n "$PLAN" --location "$LOCATION" --is-linux --sku F1 -o none
else
  PLAN_JSON=$(azc appservice plan show -g "$GROUP" -n "$PLAN" -o json)
  jq -e '.sku.name == "F1" and .reserved == true' <<< "$PLAN_JSON" >/dev/null || { echo "O plano existente precisa ser Linux/F1." >&2; exit 1; }
fi
APP_ID=$(azc webapp list -g "$GROUP" --query "[?name=='$APP'].id | [0]" -o tsv)
if [[ -z "$APP_ID" ]]; then
  azc webapp create -g "$GROUP" -n "$APP" --plan "$PLAN" --runtime 'JAVA:21-java21' --https-only true -o none
else
  EXPECTED_PLAN=$(azc appservice plan show -g "$GROUP" -n "$PLAN" --query id -o tsv)
  ACTUAL_PLAN=$(azc webapp show -g "$GROUP" -n "$APP" --query serverFarmId -o tsv)
  [[ "${EXPECTED_PLAN,,}" == "${ACTUAL_PLAN,,}" ]] || { echo "O destino usa outro plano; execução interrompida." >&2; exit 1; }
  azc webapp update -g "$GROUP" -n "$APP" --https-only true -o none
fi
azc webapp config set -g "$GROUP" -n "$APP" --linux-fx-version 'JAVA|21-java21' --min-tls-version 1.2 --ftps-state Disabled --always-on false -o none
azc webapp config appsettings set -g "$GROUP" -n "$APP" --settings @"$SETTINGS" -o none
IPS=$(azc webapp show -g "$GROUP" -n "$APP" --query possibleOutboundIpAddresses -o tsv)
[[ -n "$IPS" && "$IPS" != "None" ]] || { echo "Nenhum IP de saída encontrado." >&2; exit 1; }
EXISTING_IPS=$(azc sql server firewall-rule list -g "$GROUP" -s "$SQL" --query '[?startIpAddress==endIpAddress].startIpAddress' -o tsv)
IFS=',' read -ra ADDRESSES <<< "$IPS"
for IP in "${ADDRESSES[@]}"; do
  if ! grep -Fxq "$IP" <<< "$EXISTING_IPS"; then
    azc sql server firewall-rule create -g "$GROUP" -s "$SQL" -n "DimDimWeb-${IP//./-}" --start-ip-address "$IP" --end-ip-address "$IP" -o none
  fi
done
if $DEPLOY; then
  azc webapp deploy -g "$GROUP" -n "$APP" --src-path "$JAR" --type jar --timeout 600000
fi
DOMAIN=$(azc webapp show -g "$GROUP" -n "$APP" --query defaultHostName -o tsv)
echo "Configuração concluída. Aplicação: https://$DOMAIN/pedidos"
echo "Confirme a página e a telemetria; configuração concluída não comprova recebimento de dados."
