# Ejecutar desde la raiz del backend, PowerShell 5.1+.
# No utiliza -DskipTests. Asegurar entorno de pruebas aislado previamente.
$ErrorActionPreference = 'Stop'
$servicios = @('authentication','clientes','ventas','inventario','finanzas','alertas','kpis','reportes','api-gateway','kafka-admin','rabbit-admin')
$errores = @()
foreach ($nombre in $servicios) {
  Push-Location $nombre
  try {
    Write-Host "`n========= $nombre =========" -ForegroundColor Cyan
    if (Test-Path 'mvnw.cmd') { & .\mvnw.cmd clean verify } else { & mvn clean verify }
    if ($LASTEXITCODE -ne 0) { $errores += $nombre; Write-Host "FALLO: $nombre" -ForegroundColor Red }
    else { Write-Host "OK: $nombre" -ForegroundColor Green }
  } finally { Pop-Location }
}
Write-Host "`nResumen: $($servicios.Count - $errores.Count) de $($servicios.Count) completados."
if ($errores.Count -gt 0) { Write-Host "Fallidos: $($errores -join ', ')" -ForegroundColor Red; exit 1 }
