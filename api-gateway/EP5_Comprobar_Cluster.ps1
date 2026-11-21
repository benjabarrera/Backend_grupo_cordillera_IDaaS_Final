# Ejecucion en PowerShell desde la carpeta api-gateway.
$ErrorActionPreference = 'Stop'
if (-not (Test-Path '.env')) { throw 'Crea api-gateway/.env basandote en .env.example' }
$line = Get-Content '.env' | Where-Object { $_ -match '^RABBITMQ_ERLANG_COOKIE=.+$' }
if (-not $line) { throw 'Falta RABBITMQ_ERLANG_COOKIE en .env' }
$cluster = 'docker-compose.rabbit-cluster.yml'
Write-Host 'Validando Compose del cluster aislado...'
docker compose -f $cluster config --quiet
if ($LASTEXITCODE -ne 0) { throw 'Fallo docker compose config' }
Write-Host 'Levantando dos nodos aislados (no afecta al RabbitMQ de negocio)...'
docker compose -f $cluster up -d
if ($LASTEXITCODE -ne 0) { throw 'Fallo Docker Compose up' }
docker compose -f $cluster ps
Write-Host 'Verifica que ambos nodos aparezcan en Running Nodes; puede requerir revisar logs tras el primer arranque.'
docker compose -f $cluster exec rabbitmq-cluster-1 rabbitmqctl cluster_status
if ($LASTEXITCODE -ne 0) { throw 'No se pudo obtener cluster_status' }
Write-Host 'Para verificar el nodo 2: docker compose -f docker-compose.rabbit-cluster.yml exec rabbitmq-cluster-2 rabbitmqctl cluster_status'
