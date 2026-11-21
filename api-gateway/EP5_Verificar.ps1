$ErrorActionPreference = 'Stop'
$files = @('docker-compose.yml','docker-compose.messaging.yml',
           'docker-compose.integracion-kafka.yml','docker-compose.ep5-final.yml')
foreach ($file in $files) {
    if (-not (Test-Path $file)) { throw "Falta $file en la carpeta api-gateway" }
}
Write-Host '[1/3] Validando Docker Compose...' -ForegroundColor Cyan
& docker compose -f $files[0] -f $files[1] -f $files[2] -f $files[3] config --quiet
if ($LASTEXITCODE -ne 0) { throw 'Docker Compose invalido' }
Write-Host '[2/3] Servicios activos...' -ForegroundColor Cyan
& docker compose -f $files[0] -f $files[1] -f $files[2] -f $files[3] ps
Write-Host '[3/3] Topics Kafka...' -ForegroundColor Cyan
& docker compose -f $files[0] -f $files[1] -f $files[2] -f $files[3] exec -T kafka kafka-topics --bootstrap-server kafka:29092 --list
Write-Host 'Revision de infraestructura finalizada (confirma personalmente el estado Up de los servicios).' -ForegroundColor Green
