# Solo si tienes un JWT de Entra ID con rol SUPER_ADMIN y el EP5_ADMIN_TOKEN local.
$ErrorActionPreference = 'Stop'
$jwt = Read-Host 'JWT valido de Microsoft Entra ID (SUPER_ADMIN)'
$key = Read-Host 'EP5_ADMIN_TOKEN definido en .env'
$headers = @{ Authorization="Bearer $jwt"; 'X-EP5-Admin-Key'=$key }
Write-Host '[KAFKA] Topics:'
Invoke-RestMethod 'http://localhost:8080/admin/kafka/topics' -Headers $headers | ConvertTo-Json -Depth 7
Write-Host '[RABBIT] Colas:'
Invoke-RestMethod 'http://localhost:8080/admin/rabbit/queues' -Headers $headers | ConvertTo-Json -Depth 7
# Crear topic temporal (solo entorno laboratorio).
$body = @{nombre='ep5-demo'; particiones=3; replicacion=2; retentionMs=3600000; cleanupPolicy='delete'} | ConvertTo-Json
Write-Host 'Para CREAR un topic temporal: '
Write-Host 'Invoke-RestMethod -Method Post -Uri http://localhost:8080/admin/kafka/topics -Headers $headers -ContentType application/json -Body $body'
