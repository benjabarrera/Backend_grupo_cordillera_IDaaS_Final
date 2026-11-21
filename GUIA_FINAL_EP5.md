# EP5 Grupo Cordillera — Guía de ejecución de pendientes

Esta distribución parte del **backend y frontend entregados por el equipo**. Conserva el código de Kafka, RabbitMQ, Spring Boot, Entra ID y PostgreSQL Azure. Las correcciones realizadas se describen al final.

**Importante:** los archivos son código fuente listo para configurar y ejecutar. No contienen las contraseñas reales. Debes instalar Docker Desktop, Java 17+ y Maven, y comprobar los resultados en tu computadora. No se han ejecutado contenedores ni pruebas end-to-end en este entorno.

## Preparación — antes de comenzar

1. Descomprime **ambos ZIP en carpetas hermanas**, manteniendo estos nombres: `backend-grupocordillera-IDaaS-main` y `Frontend-GrupoCordillera-Angular-IDaaS-main`.
2. Respalda el proyecto y los datos Docker existentes. No utilices `docker compose down -v`.
3. En **PowerShell**, entra al backend:

   ```powershell
   cd "C:\Users\bb205\OneDrive\Desktop\backend-grupocordillera-IDaaS-main\api-gateway"
   ```

   Ajusta la ruta si lo descomprimiste en otra ubicación.
4. Abre `api-gateway/.env.example`; crea una copia llamada **`.env`** en la misma carpeta. Escribe ahí tus **propias** credenciales locales y las que tenga tu compañero para Microsoft Entra ID y Azure PostgreSQL. Nunca subas `.env` al repositorio.
5. Genera una cookie RabbitMQ segura (sin publicarla), por ejemplo en PowerShell:

   ```powershell
   $b = New-Object byte[] 32
   $r = [System.Security.Cryptography.RandomNumberGenerator]::Create()
   $r.GetBytes($b)
   $r.Dispose()
   [Convert]::ToBase64String($b)
   ```

   Introduce el valor privado generado en `RABBITMQ_ERLANG_COOKIE` dentro de `.env`.

## PASO 1 — Comprobar el clúster RabbitMQ (captura 01)

**Cambio importante:** la demostración ahora está **aislada**. No mezcles `docker-compose.rabbit-cluster.yml` con los otros Compose. Así no se pisan los volúmenes del RabbitMQ de negocio.

Desde `api-gateway`:

```powershell
docker compose -f docker-compose.rabbit-cluster.yml config --quiet
docker compose -f docker-compose.rabbit-cluster.yml up -d
docker compose -f docker-compose.rabbit-cluster.yml ps
docker compose -f docker-compose.rabbit-cluster.yml exec rabbitmq-cluster-1 rabbitmqctl cluster_status
```

Busca **dos nodos** en `Running Nodes`: `rabbit@rabbitmq-cluster-1` y `rabbit@rabbitmq-cluster-2`. También puedes revisar la interfaz del laboratorio en http://localhost:15673 y http://localhost:15674. Si aparece uno, comprueba `docker compose -f docker-compose.rabbit-cluster.yml logs --tail=100` y el valor de cookie (sin enseñarlo).

**CAPTURA 01:** `01_RabbitMQ_Cluster.png` mostrando `cluster_status` con ambos nodos. No basta con mostrar los dos contenedores.

> Esta prueba cumple la existencia y comprobación de un Compose de clúster. El RabbitMQ de negocio continúa separado en 15672: la topología de auditoría y su DLQ no se copian automáticamente al clúster de demostración. No presentes la DLQ de negocio como si estuviera replicada allí.

## PASO 2 — Levantar el sistema principal (sin Keycloak) y probar DLQ (captura 02)

La versión original del frontend usa **Microsoft Entra ID**. No es necesario Keycloak si tu compañero dispone de una cuenta autorizada.

En otra terminal PowerShell, desde `api-gateway`:

```powershell
$dc = @('-f','docker-compose.yml','-f','docker-compose.messaging.yml','-f','docker-compose.integracion-kafka.yml','-f','docker-compose.ep5-final.yml')
docker compose @dc config --quiet
docker compose @dc up -d --build zookeeper kafka kafka-2 kafka-ui rabbitmq ventas inventario ms-alertas kafka-admin rabbit-admin api-gateway
docker compose @dc ps
```

Si tu compañero ya preparó la BD Azure para todos los servicios, agrega **ANTES de levantar los servicios** su override:

```powershell
$dc += @('-f','docker-compose.azure-db.yml')
docker compose @dc config --quiet
```

El override cloud exige variables `AZURE_PG_HOST`, `AZURE_PG_USER`, `AZURE_PG_PASSWORD` reales y que las bases existan. No las sustituyas por valores ficticios. Si ya existen volúmenes de PostgreSQL local y cambias sus contraseñas en `.env`, las contraseñas internas persistentes podrían no cambiar: usa las credenciales originales para esos volúmenes o migra de forma controlada.

Abre RabbitMQ Management en http://localhost:15672 (no 15673) y comprueba las colas `cordillera.auditoria` y `cordillera.auditoria.dlq` y los exchanges correspondientes. Deben estar declarados tras iniciar `ventas` y `ms-alertas`.

Para enviar un evento de auditoría inválido:

```powershell
Unblock-File .\EP5_Prueba_DLQ_Rabbit.ps1
.\EP5_Prueba_DLQ_Rabbit.ps1
```

Introduce las credenciales de RabbitMQ cuando lo pida. Si aparece `routed: true`, inspecciona **Queues and Streams → cordillera.auditoria.dlq → Get messages** y los logs:

```powershell
docker compose @dc logs --tail=100 ms-alertas
```

**CAPTURA 02:** `02_RabbitMQ_DLQ.png`: un mensaje fallido visible en la DLQ y, si puedes, el error/reintento en logs.

## PASO 3 — Probar el administrador Kafka (capturas 03–05)

Importa en Postman `api-gateway/EP5_Administracion_Postman.postman_collection.json`.

La colección incluye variables `gatewayUrl` (default http://localhost:8080), `accessToken` y `adminKey` **vacías**. Configura:
- `accessToken`: access token **real** emitido para la API de Grupo Cordillera, de una cuenta Microsoft Entra ID autorizada como `SUPER_ADMIN`.
- `adminKey`: valor privado `EP5_ADMIN_TOKEN` de `.env`.

No pegues ninguna credencial en capturas, GitHub ni chats. Si GET devuelve 403, revisa rol y clave; no desactives la seguridad.

Ejecuta en el orden establecido por la colección:
1. **01 Kafka listar topics** — GET (200 esperado).
2. **02 Kafka crear** `ep5-validacion-final`, 3 particiones, 2 réplicas, retención 1 hora — POST. Verifica en http://localhost:8090. **CAPTURA 03** `03_Kafka_Crear.png`.
3. **03 Kafka modificar retención** 1 a 2 horas — PATCH y **04 Kafka consultar topic** — GET con `retention.ms=7200000`. **CAPTURA 04** `04_Kafka_Modificar.png`.
4. **05 Kafka listar consumer groups** — GET; **06 Kafka offsets de ventas** — GET, siempre que los consumidores hayan sido ejecutados.
5. **07 Kafka eliminar topic temporal** — DELETE con `confirm=true`, **solo después de las capturas**. Esperado 204. **CAPTURA 05** `05_Kafka_Eliminar.png`.

Nunca borres `cordillera-ventas`, `cordillera-inventario` o los topics de errores del negocio.

## PASO 4 — Probar el administrador RabbitMQ (capturas 06–08)

En la misma colección Postman, conserva JWT y la clave administrativa y ejecuta:
1. **08 Rabbit listar colas** (GET).
2. **09 Rabbit crear exchange** (PUT `/admin/rabbit/exchanges/ep5.demo`).
3. **10 Rabbit crear cola** (PUT `/admin/rabbit/queues/ep5.demo`). Busca ambos en http://localhost:15672. **CAPTURA 06** `06_Rabbit_Crear.png`.
4. **11 Rabbit crear binding** (POST con exchange/queue `ep5.demo`, routing key `ep5.audit`), verifica en Exchange → Bindings. **CAPTURA 07** `07_Rabbit_Binding.png`.
5. **13 Rabbit eliminar binding**, **14 eliminar cola**, **15 eliminar exchange**. Verifica que desaparecieron, **CAPTURA 08** `08_Rabbit_Eliminar.png`.

Todos estos recursos de prueba tienen el prefijo permitido `ep5.` y no son las colas de la aplicación.

## PASO 5 — Ejecutar pruebas automáticas (capturas 09–10)

Desde la **raíz** del backend en PowerShell, con datos de prueba aislados:

```powershell
.\EP5_Verificar_Builds.ps1
```

Este script ejecuta `clean verify` **sin omitir pruebas** en los 11 proyectos y muestra cuáles fallaron. Las dependencias externas pueden necesitar Docker/BD de pruebas. Comprueba también `target/surefire-reports` de cada servicio. No afirmes que todas las pruebas pasaron si alguno falla.

En la carpeta del frontend:

```powershell
npm ci
npm run build
```

**CAPTURA 09:** resultados Maven `BUILD SUCCESS` y pruebas ejecutadas. **CAPTURA 10:** compilación Angular sin errores bloqueantes. Las advertencias CSS no equivalen automáticamente a fallo.

## PASO 6 — Verificar Entra ID, Azure y el flujo de negocio (usar capturas ya existentes)

1. Tu compañero inicia el frontend original con `npm start` y se autentica **con su propia cuenta autorizada**, no necesitas su contraseña.
2. Verifica que el Gateway acepte el JWT y que las pantallas de Ventas e Inventario muestren datos.
3. Si usas cloud, comprueba conexión PostgreSQL Azure mediante la configuración existente y consulta un registro de prueba en el proveedor sin mostrar credenciales.
4. Registra una venta de prueba, comprueba el evento en `cordillera-ventas`, recepción en `ms-alertas`, actualización de inventario y auditoría RabbitMQ. Si las capturas del ZIP original ya muestran el mismo flujo, reutilízalas.

## PASO 7 — GitHub y entrega (capturas 11–12)

1. **No subas el ZIP original sin revisar:** incluía un archivo `.env` con secretos. Los ZIP limpios adjuntos no incluyen ese archivo ni los artefactos `target`, cachés o `dist`.
2. Copia solo credenciales privadas a tu `.env` local. Revisa también las variables en los Compose y sus defaults. Si compartiste secretos reales fuera del grupo autorizado o los publicaste, rótalos.
3. En backend y frontend por separado: `git status`, `git add .`, `git diff --cached --name-only`, comprueba que no aparezcan secretos y luego `git commit` y `git push` al remoto de tu grupo. **No** ejecutes `git push --force`.
4. **CAPTURA 11:** GitHub del backend. **CAPTURA 12:** GitHub del frontend.
5. Presenta arquitectura, Kafka, RabbitMQ/DLQ, administración, Entra ID, PostgreSQL Azure, pruebas, resultados y enlaces. Entrega ambos repositorios mediante AVA y correo según la EP5.

## Cambios aplicados a esta entrega limpia

- Mantiene el backend y frontend existentes de los dos ZIP del usuario.
- **No incluye** los archivos `.env` originales ni compilados `.class/.jar`, directorios `target`, `dist`, `.angular/cache` ni contraseñas privadas.
- Incluye `.env.example` con **valores ficticios**; debes crear el `.env` privado.
- **Corrige el Compose de demostración del clúster:** ahora es independiente de la mensajería principal, con dos nodos y volúmenes aislados.
- Ajusta credenciales de PostgreSQL local/Metabase/Sonar de los Compose a variables del `.env`, y elimina contraseñas locales de respaldo en propiedades de aplicación.
- Aporta `EP5_Comprobar_Cluster.ps1`, `EP5_Verificar_Builds.ps1` y una colección Postman para la administración protegida.
- En el frontend, corrige `verccel.json` a `vercel.json` (reglas SPA). **No cambia MSAL/Entra**.
- La configuración de producción Angular (`environment.prod.ts`) contiene marcadores de URL y requiere definir URL real del frontend y Gateway **antes de desplegar a Internet**. La ejecución local no necesita esos cambios.

## Limitaciones de validación

Se ha revisado la estructura, la sintaxis YAML/XML/JSON y los nombres de los endpoints. **No se pudo ejecutar Docker Desktop ni una compilación Maven/Angular completa en este entorno.** Los resultados, permisos de Microsoft Entra y acceso real a Azure deben comprobarse en Windows. No presente datos de prueba como resultados comprobados hasta obtener las capturas.
