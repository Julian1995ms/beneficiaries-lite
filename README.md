# beneficiaries-lite

Microservicio de ejemplo para administrar beneficiarios de transferencias bancarias. Implementa los cuatro casos de uso solicitados en la prueba técnica: registrar, consultar activos por cliente, consultar por ID y desactivar mediante baja lógica.

## Stack tecnológico

- Java 17.
- Quarkus 3.40.1.2 LTS.
- Maven 3.9.16 mediante Maven Wrapper 3.3.4.
- Quarkus REST Jackson (`quarkus-rest-jackson`).
- Hibernate Validator.
- Hibernate ORM con `EntityManager`.
- H2 en memoria.
- CDI.
- SmallRye OpenAPI y Swagger UI.
- SmallRye Health.
- Lombok 1.18.48.
- JUnit 5, Mockito, `@QuarkusTest` y RestAssured.
- JaCoCo.
- Docker opcional.

> El documento original menciona `quarkus-resteasy-reactive-jackson`. En Quarkus 3.40 la denominación vigente es `quarkus-rest-jackson`; se utiliza esa extensión manteniendo el mismo propósito funcional.

## Prerrequisitos

- JDK 17 instalado y configurado.
- IntelliJ IDEA recomendado.
- Git únicamente si se desea versionar o clonar el repositorio.
- Docker opcional.

No se requiere Maven global ni una base de datos externa. El Maven Wrapper descargará Maven 3.9.16 en la primera ejecución.

## Compilación

Linux/macOS:

```bash
./mvnw clean package
```

Windows PowerShell o CMD:

```powershell
.\mvnw.cmd clean package
```

## Ejecución en desarrollo

Linux/macOS:

```bash
./mvnw quarkus:dev
```

Windows:

```powershell
.\mvnw.cmd quarkus:dev
```

La aplicación queda disponible en `http://localhost:8080`.

## Ejecución empaquetada

Después de compilar:

```bash
java -jar target/quarkus-app/quarkus-run.jar
```

Quarkus utiliza empaquetado `fast-jar`, por lo que se debe conservar completo el directorio `target/quarkus-app`.

## Arquitectura

La solución utiliza arquitectura hexagonal con tres áreas principales:

```text
mx.com.empresa.backend
├── application
│   ├── dto
│   ├── exception
│   ├── port
│   │   ├── in
│   │   └── out
│   └── service
├── domain
│   ├── model
│   └── validation
└── infrastructure
    ├── adapter
    │   ├── in/rest
    │   └── out/persistence
    ├── config
    └── exception
```

Flujo principal:

```text
Cliente HTTP
    ↓
BeneficiaryResource
    ↓
BeneficiaryUseCase
    ↓
BeneficiaryService
    ↓
BeneficiaryRepositoryPort / TransactionExecutor
    ↓
BeneficiaryPersistenceAdapter / JakartaTransactionExecutor
    ↓
Hibernate ORM / EntityManager
    ↓
H2
```

El dominio no depende de JPA, HTTP ni infraestructura. Application define los puertos y las reglas de aplicación; Infrastructure implementa persistencia, transporte HTTP, transacciones y traducción de excepciones.

## API REST

### Registrar beneficiario

```http
POST /api/v1/beneficiaries
```

Respuesta exitosa: `201 Created`, incluyendo encabezado `Location`.

```bash
curl -X POST http://localhost:8080/api/v1/beneficiaries \
  -H "Content-Type: application/json" \
  -d '{
    "customerId":"12345678",
    "alias":"Proveedor papelería",
    "holderName":"Papelería La Estrella SA de CV",
    "clabe":"032180000118359719",
    "maxAmount":50000.00,
    "email":"pagos@laestrella.mx"
  }'
```

### Consultar beneficiarios activos

```http
GET /api/v1/beneficiaries?customerId=12345678
```

```bash
curl "http://localhost:8080/api/v1/beneficiaries?customerId=12345678"
```

Si no existen resultados se devuelve `200 OK` con `[]`.

### Consultar beneficiario por ID

```http
GET /api/v1/beneficiaries/{id}
```

```bash
curl http://localhost:8080/api/v1/beneficiaries/1
```

La consulta permite recuperar beneficiarios activos e inactivos.

### Desactivar beneficiario

```http
PATCH /api/v1/beneficiaries/{id}/deactivate
```

```bash
curl -X PATCH http://localhost:8080/api/v1/beneficiaries/1/deactivate
```

Respuesta exitosa: `204 No Content`.

### Nota para PowerShell

PowerShell puede interpretar `curl` como un alias dependiendo de la versión. Si ocurre, utilice `curl.exe` o Postman.

## Reglas de negocio

1. Una misma CLABE no puede registrarse dos veces para el mismo `customerId`, incluso si el registro anterior está `INACTIVE`.
2. La CLABE debe tener 18 dígitos y cumplir el algoritmo de dígito verificador con pesos 3, 7 y 1.
3. Cada cliente puede tener máximo 10 beneficiarios activos.
4. Una consulta o desactivación sobre un ID inexistente devuelve `404`.
5. Desactivar nuevamente un beneficiario inactivo devuelve `409`.
6. `id`, `bankCode`, `status` y `createdAt` son generados por el sistema.
7. `bankCode` corresponde a los primeros tres dígitos de la CLABE.

## Validaciones de entrada

- `customerId`: exactamente 8 dígitos ASCII.
- `alias`: obligatorio, de 3 a 30 caracteres.
- `holderName`: obligatorio, máximo 100 caracteres.
- `clabe`: exactamente 18 dígitos ASCII y dígito verificador válido.
- `maxAmount`: mayor a cero, máximo 17 enteros y 2 decimales.
- `email`: opcional, máximo 254 caracteres y formato válido.
- `alias` y `holderName` se recortan con `trim()` durante la deserialización, antes de Bean Validation.
- Las propiedades JSON desconocidas se rechazan con `400`.

## Manejo de errores

Todos los errores de la aplicación usan una estructura común:

```json
{
  "code": "BEN-409",
  "message": "La CLABE ya está registrada para este cliente",
  "timestamp": "2026-11-20T10:30:00",
  "details": []
}
```

Ejemplo de validación:

```json
{
  "code": "BEN-400",
  "message": "La solicitud contiene datos inválidos",
  "timestamp": "2026-11-20T10:30:00",
  "details": [
    {
      "field": "customerId",
      "message": "El identificador del cliente debe contener 8 dígitos"
    }
  ]
}
```

Códigos principales:

| Código | HTTP |
|---|---:|
| BEN-400 | 400 |
| BEN-404 | 404 |
| BEN-409 | 409 |
| BEN-422 | 422 |
| BEN-500 | 500 |

Los timestamps se generan en UTC mediante `LocalDateTime.now(ZoneOffset.UTC)`. `LocalDateTime` no conserva la zona; UTC es la convención de la aplicación.

## Persistencia

La aplicación usa Hibernate ORM, `EntityManager` y H2 en memoria.

Tabla: `beneficiaries`.

Restricciones principales:

- PK autogenerada sobre `id`.
- `UNIQUE (customer_id, clabe)` sin condición por estado.
- Índice compuesto `(customer_id, status)`.
- `max_amount DECIMAL(19,2)`.
- `email VARCHAR(254)`.

El esquema se crea al iniciar la aplicación y la información se pierde al terminar el proceso o recrear el esquema.

## Concurrencia y transacciones

El límite de 10 activos requiere coordinación adicional a `@Transactional`.

`BeneficiaryService` mantiene un `ReentrantLock` por `customerId`. Mientras el lock está adquirido, ejecuta las validaciones de duplicidad, el conteo y la escritura dentro de `TransactionExecutor`. El adaptador `JakartaTransactionExecutor` utiliza `@Transactional(REQUIRES_NEW)`.

El interceptor transaccional completa commit o rollback antes de devolver el control al servicio; por ello el servicio libera el lock únicamente cuando la transacción ya concluyó.

La misma coordinación se aplica a la baja lógica. La restricción única de base de datos permanece como segunda defensa.

**Limitación:** este mecanismo protege una sola instancia del microservicio. No es un lock distribuido y no pretende resolver concurrencia entre varias réplicas.

## Logs seguros

Se usan logs en operaciones relevantes. No se registran bodies completos, nombres de titulares ni correos. Cuando se registra una CLABE se muestran únicamente sus últimos cuatro dígitos, por ejemplo:

```text
**************9719
```

Los logs de parámetros SQL permanecen deshabilitados.

## OpenAPI, Swagger y salud

- Swagger UI: `http://localhost:8080/q/swagger-ui`
- OpenAPI: `http://localhost:8080/q/openapi`
- Health: `http://localhost:8080/q/health`

Swagger UI se incluye también en la aplicación empaquetada mediante `quarkus.swagger-ui.always-include=true`.

## Pruebas

Ejecutar toda la suite:

Linux/macOS:

```bash
./mvnw clean verify
```

Windows:

```powershell
.\mvnw.cmd clean verify
```

La suite contiene:

- Unitarias del validador de CLABE.
- Unitarias de `BeneficiaryService` con el puerto de persistencia mockeado.
- Integración con `@QuarkusTest`, RestAssured y H2 real.
- Escenarios de validación, JSON inválido, propiedades desconocidas, baja lógica, duplicidad y límite de activos.
- Pruebas concurrentes para el décimo beneficiario y para dos altas simultáneas con la misma CLABE.
- Health check.

## Cobertura JaCoCo

`quarkus-jacoco` cubre las pruebas Quarkus y `jacoco-maven-plugin` agrega instrumentación a las unitarias normales. Ambos escriben en el mismo archivo de ejecución para generar un reporte combinado.

Después de `clean verify`, el reporte HTML se encuentra en:

```text
target/jacoco-report/index.html
```

No se establece un porcentaje mínimo arbitrario; la prioridad es cubrir reglas y comportamientos relevantes.

## Postman

Colección incluida en:

```text
postman/beneficiaries-lite.postman_collection.json
```

Variables:

- `baseUrl`: `http://localhost:8080`
- `customerId`: `12345678`
- `beneficiaryId`: se actualiza automáticamente después de un POST exitoso.

La colección contiene los cuatro endpoints, health check y ejemplos de error.

## Docker

Docker es opcional.

```bash
docker build -t beneficiaries-lite .
docker run --rm -p 8080:8080 beneficiaries-lite
```

El Dockerfile está preparado para compilar y ejecutar la aplicación en modo JVM con Java 17 y utiliza un usuario numérico sin privilegios en la imagen de runtime.

## Decisiones técnicas

- Arquitectura hexagonal para aislar dominio, aplicación y adaptadores.
- Inyección por constructor.
- DTOs, dominio y entidad JPA independientes.
- Hibernate ORM con `EntityManager`; no se usa Panache.
- H2 en memoria para mantener la prueba autocontenida.
- `quarkus-rest-jackson` como nombre vigente de RESTEasy Reactive Jackson.
- Demarcación transaccional separada mediante un puerto para no acoplar Application a infraestructura.
- Lock por cliente para mantener la transacción completa dentro de la sección crítica.
- Restricción única de base de datos como protección adicional.
- Mappers específicos para Bean Validation y procesamiento JSON para mantener el contrato de errores.

## Supuestos funcionales implementados

1. La CLABE no se reutiliza para el mismo cliente aunque el registro esté inactivo.
2. GET por ID devuelve también registros inactivos.
3. Desactivación exitosa devuelve `204`.
4. Consulta sin activos devuelve `200` y `[]`.
5. `customerId` del GET exige 8 dígitos y conserva ceros iniciales.
6. Fechas generadas con convención UTC.
7. `maxAmount` usa 17 enteros + 2 decimales; `email` máximo 254.
8. `alias` y `holderName` se normalizan antes de validar.
9. Las propiedades JSON desconocidas se rechazan.

## Fuera de alcance

No se implementan autenticación, autorización, JWT, clientes REST externos, fault tolerance, catálogos bancarios externos, reactivación, actualización general, eliminación física, paginación, Redis, servicios distribuidos, base de datos externa, Docker Compose, compilación nativa ni GitHub Actions.

## IntelliJ IDEA en Windows

1. Instale un JDK 17, por ejemplo Eclipse Temurin u otra distribución compatible.
2. Verifique en PowerShell:
   ```powershell
   java -version
   javac -version
   ```
3. Si es necesario, configure `JAVA_HOME` apuntando al directorio del JDK 17 y agregue `%JAVA_HOME%\bin` al `PATH`.
4. En IntelliJ IDEA seleccione **File > Open** y abra la carpeta `beneficiaries-lite` o su `pom.xml`.
5. Confirme que el proyecto se importe como Maven.
6. En **File > Project Structure > Project**, seleccione JDK 17 como Project SDK.
7. En **Settings > Build, Execution, Deployment > Build Tools > Maven**, utilice el Maven Wrapper y JDK 17 para Maven.
8. Recargue el proyecto Maven y verifique que no existan dependencias sin resolver.
9. Ejecute `mvnw.cmd clean verify` desde la terminal integrada.
10. Ejecute `mvnw.cmd quarkus:dev` para desarrollo.
11. Abra Swagger UI en `http://localhost:8080/q/swagger-ui`.
12. Importe la colección de `postman/` en Postman.
13. Después de las pruebas abra `target/jacoco-report/index.html` en el navegador.
14. Si desea usar Docker, ejecute los comandos de la sección Docker.

## Limitaciones reales

- H2 es temporal y se reinicia con el proceso/esquema.
- La coordinación de concurrencia es deliberadamente local a una sola instancia.
- No existe reactivación de beneficiarios ni integración con sistemas externos.
