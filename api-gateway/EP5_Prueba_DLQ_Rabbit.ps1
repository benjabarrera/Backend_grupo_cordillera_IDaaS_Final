# Prueba CONTROLADA de RabbitMQ, ejecutarla desde PowerShell.
# Usa HTTP API solo para prueba: produccion usa RabbitTemplate.
$ErrorActionPreference = 'Stop'
$user = Read-Host 'Usuario de RabbitMQ (normalmente cordillera)'
$secure = Read-Host 'Contrasena RabbitMQ' -AsSecureString
$bstr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure)
try { $pass = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($bstr) }
finally { [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($bstr) }
$basic = [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes("${user}:${pass}"))
$headers = @{ Authorization="Basic $basic" }
$event = '{"eventId":"ep5-invalido","tipo":"VENTA_REGISTRADA","ventaId":null,"skuProducto":"EP5-ERROR","montoTotal":10.0}'
$body = @{ properties = @{delivery_mode=2}; routing_key='ventas.audit'; payload=$event; payload_encoding='string' } | ConvertTo-Json -Depth 6
$result = Invoke-RestMethod -Method Post -Uri 'http://localhost:15672/api/exchanges/%2F/cordillera.eventos/publish' -Headers $headers -ContentType 'application/json' -Body $body
$result | ConvertTo-Json
Write-Host 'Si routed=true y Alertas esta ejecutando, comprueba la DLQ en RabbitMQ Management.'
