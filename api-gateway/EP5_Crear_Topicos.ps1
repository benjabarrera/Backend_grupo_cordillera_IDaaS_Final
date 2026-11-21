# Ejecutar DESDE api-gateway con los dos archivos docker-compose.yml y docker-compose.messaging.yml.
# Antes: docker compose -f docker-compose.yml -f docker-compose.messaging.yml up -d zookeeper kafka kafka-2 kafka-ui rabbitmq
$ErrorActionPreference = 'Stop'
$composeArgs = @('-f', 'docker-compose.yml', '-f', 'docker-compose.messaging.yml')

function KafkaTopics([string[]] $toolArgs) {
    & docker compose @composeArgs exec -T kafka kafka-topics @toolArgs
    if ($LASTEXITCODE -ne 0) { throw 'Error de Kafka al crear o consultar topics. Verifica docker compose ps y logs.' }
}

KafkaTopics -toolArgs @('--bootstrap-server', 'kafka:29092', '--create', '--if-not-exists', '--topic', 'cordillera-ventas', '--partitions', '3', '--replication-factor', '2', '--config', 'retention.ms=604800000', '--config', 'retention.bytes=268435456', '--config', 'cleanup.policy=delete')
KafkaTopics -toolArgs @('--bootstrap-server', 'kafka:29092', '--create', '--if-not-exists', '--topic', 'cordillera-inventario', '--partitions', '3', '--replication-factor', '2', '--config', 'retention.ms=259200000', '--config', 'retention.bytes=52428800', '--config', 'cleanup.policy=delete')
KafkaTopics -toolArgs @('--bootstrap-server', 'kafka:29092', '--create', '--if-not-exists', '--topic', 'cordillera-ventas-dlt', '--partitions', '3', '--replication-factor', '2', '--config', 'retention.ms=604800000', '--config', 'cleanup.policy=delete')
KafkaTopics -toolArgs @('--bootstrap-server', 'kafka:29092', '--create', '--if-not-exists', '--topic', 'cordillera-inventario-dlt', '--partitions', '3', '--replication-factor', '2', '--config', 'retention.ms=604800000', '--config', 'cleanup.policy=delete')

Write-Host '--- TOPICS ---'
KafkaTopics -toolArgs @('--bootstrap-server', 'kafka:29092', '--list')
Write-Host '--- DESCRIPCION: ventas ---'
KafkaTopics -toolArgs @('--bootstrap-server', 'kafka:29092', '--describe', '--topic', 'cordillera-ventas')
Write-Host '--- DESCRIPCION: inventario ---'
KafkaTopics -toolArgs @('--bootstrap-server', 'kafka:29092', '--describe', '--topic', 'cordillera-inventario')
Write-Host '--- CONFIGURACION: ventas ---'
& docker compose @composeArgs exec -T kafka kafka-configs --bootstrap-server kafka:29092 --entity-type topics --entity-name cordillera-ventas --describe
if ($LASTEXITCODE -ne 0) { throw 'No se pudo consultar configuracion del topic.' }
Write-Host '--- CONFIGURACION: inventario ---'
& docker compose @composeArgs exec -T kafka kafka-configs --bootstrap-server kafka:29092 --entity-type topics --entity-name cordillera-inventario --describe
if ($LASTEXITCODE -ne 0) { throw 'No se pudo consultar configuracion del topic.' }
