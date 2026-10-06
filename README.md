# DeepBlue Rescue

Laboratorio de persistencia para la plataforma DeepBlue Rescue.

## Qué es esto

DeepBlue Rescue es una plataforma para organizaciones que rescatan y rehabilitan fauna marina. En este proyecto construí la capa de persistencia completa: modelo relacional, migraciones con Flyway, entidades JPA, repositories, Query Methods, consultas JPQL y pruebas de integración contra PostgreSQL real usando Testcontainers.

Stack:

- Java 21
- Spring Boot 4
- Spring Data JPA + Hibernate
- Flyway
- PostgreSQL
- Testcontainers

## Modelo de datos

Las entidades y sus relaciones:

- RescueCenter tiene muchos RescueCase (1:N)
- RescueCase tiene un Animal (1:1)
- Animal tiene un MedicalRecord (1:1)
- Animal tiene muchos Treatment (1:N)
- Specialist tiene muchos Treatment (1:N)
- Specialist tiene muchas Expertise (N:M)

## Relaciones JPA

| Relación | Tipo | Dueño | FK |
|---|---|---|---|
| RescueCenter ↔ RescueCase | 1:N | RescueCase | rescue_cases.rescue_center_id |
| RescueCase ↔ Animal | 1:1 | Animal | animals.rescue_case_id (UNIQUE) |
| Animal ↔ MedicalRecord | 1:1 | MedicalRecord | medical_records.animal_id (UNIQUE) |
| Specialist ↔ Expertise | N:M | Specialist | tabla specialist_expertise |
| Treatment → Animal | N:1 | Treatment | treatments.animal_id |
| Treatment → Specialist | N:1 | Treatment | treatments.specialist_id |

## Instrucciones para ejecutar

Se necesita Java 21, Maven y Docker para los tests.

Compilar:

    ./mvnw clean compile

Correr los tests:

    ./mvnw clean test

Los tests levantan un PostgreSQL 18 en Docker con Testcontainers. Flyway ejecuta las migraciones y Hibernate valida que el esquema coincida con las entidades.

## Migraciones Flyway

Flyway es el encargado de crear y evolucionar el esquema. Nada de ddl-auto: update ni create.

- V1__create_schema.sql — crea las 8 tablas
- V2__insert_expertise_catalog.sql — inserta el catálogo de expertise
- V3__add_tracking_device_to_animal.sql — agrega tracking_device_code a animals

Una vez aplicada, una migración no se toca. Si hay que cambiar algo, va en una migración nueva.

## Query Methods

- RescueCenterRepository.findByCode(String)
- RescueCaseRepository.findByCaseCode(String)
- RescueCaseRepository.findByStatusOrderByRescueDateAsc(RescueStatus)
- RescueCaseRepository.findByRescueCenterCode(String)
- RescueCaseRepository.findByRescueDateAfterOrderByRescueDateDesc(LocalDate)
- AnimalRepository.findByAnimalCode(String)
- AnimalRepository.findByCommonNameContainingIgnoreCase(String)
- AnimalRepository.findByRescueCaseStatus(RescueStatus)
- AnimalRepository.findByRescueCaseRescueCenterCode(String)
- ExpertiseRepository.findByNameIgnoreCase(String)
- TreatmentRepository.findByAnimalIdOrderByPerformedAtAsc(Long)

## Consultas JPQL

- SpecialistRepository.findActiveByExpertise(String)
- TreatmentRepository.findBetweenDates(LocalDateTime, LocalDateTime)
- TreatmentRepository.findByCenterCode(String)
- TreatmentRepository.findBySpecialistExpertise(String)
- AnimalRepository.findDistinctByStatusAndTreatmentExpertise(RescueStatus, String)


## Resultado

    Tests run: 24, Failures: 0, Errors: 0, Skipped: 0
    BUILD SUCCESS

## Capa de Servicio

Encima de la capa de persistencia, el proyecto incluye una capa de servicio que:

- Aplica reglas de negocio.
- Orquesta múltiples repositories.
- Controla transacciones.
- Transforma entidades a DTOs con MapStruct.

### Servicios implementados

- **RescueCaseService**
    - `findByCode(String)` — busca un caso por código
    - `findByStatus(RescueStatus)` — lista casos según estado
    - `changeStatus(String, ChangeRescueStatusRequest)` — cambia estado con validación de transición

- **TreatmentService**
    - `register(CreateTreatmentRequest)` — registra un tratamiento con 5 reglas de negocio
    - `findByAnimalCode(String)` — lista tratamientos de un animal

- **AnimalService**
    - `findByCode(String)` — busca animal por código
    - `findAnimalsInRehabilitation()` — lista animales en rehabilitación
    - `canReceiveTreatment(String)` — indica si un animal puede recibir tratamientos

### Tests

Unit tests con JUnit + Mockito + AssertJ, sin base de datos:

- `RescueCaseServiceImplTest` — 4 tests
- `TreatmentServiceImplTest` — 4 tests
- `AnimalServiceImplTest` — 7 tests

Total: 15 unit tests + 24 integration tests del laboratorio anterior.

## Capa de controladores (laboratorio 3)

Expone por HTTP las operaciones de la capa Service mediante una API REST. Los controllers solo traducen HTTP hacia la aplicación: no contienen reglas de negocio, no usan Repository y nunca retornan entidades, solo DTOs.

```
HTTP Request → Controller → Service → Repository → PostgreSQL
HTTP Response ← Controller ← Response DTO ← Service
```

### Endpoints

| Método | Endpoint | Operación | Respuesta |
|---|---|---|---|
| GET | `/api/rescue-cases/{caseCode}` | Consultar caso | 200 |
| GET | `/api/rescue-cases?status=...` | Buscar casos por estado | 200 |
| PATCH | `/api/rescue-cases/{caseCode}/status` | Cambiar estado del caso | 200 |
| GET | `/api/animals/{animalCode}` | Consultar animal | 200 |
| GET | `/api/animals/in-rehabilitation` | Animales en rehabilitación | 200 |
| GET | `/api/animals/{animalCode}/treatments` | Tratamientos del animal | 200 |
| GET | `/api/animals/{animalCode}/treatment-eligibility` | Elegibilidad para tratamiento | 200 |
| POST | `/api/treatments` | Registrar tratamiento | 201 |

Los 8 métodos de `RescueCaseService`, `TreatmentService` y `AnimalService` quedan expuestos.

### Validación de entrada

Los DTOs de request usan Bean Validation (`@Valid`):

- `ChangeRescueStatusRequest`: `status` obligatorio.
- `CreateTreatmentRequest`: `animalCode` y `specialistCode` no vacíos, `performedAt` obligatorio y no futuro, `type` obligatorio y `description` de 10 a 500 caracteres.

La validación de entrada se hace en el DTO. Las reglas de negocio (animal liberado, especialista inactivo, transición de estado inválida) se hacen en el Service.

### Contrato de errores

Todos los errores usan la misma estructura (`ErrorResponse`), generada por `GlobalExceptionHandler` (`@RestControllerAdvice`):

```json
{
  "timestamp": "2026-10-06T14:00:00",
  "status": 404,
  "error": "Not Found",
  "message": "Animal not found: AN-999",
  "details": {}
}
```

| Situación | HTTP | Excepción |
|---|---|---|
| DTO inválido | 400 | `MethodArgumentNotValidException` |
| JSON mal formado o enum inválido en el body | 400 | `HttpMessageNotReadableException` |
| Query parameter inválido | 400 | `MethodArgumentTypeMismatchException` |
| Recurso inexistente | 404 | `ResourceNotFoundException` |
| Regla de negocio incumplida | 409 | `BusinessRuleException` |
| Error inesperado | 500 | `Exception` (sin exponer detalles internos) |

En errores de validación, `details` indica qué campo falló y por qué.

### Pruebas

Los tests de controller usan `@WebMvcTest`, `@MockitoBean` y `MockMvc`. Verifican URL, método HTTP, JSON, validación, códigos de estado, cuerpo de error y delegación al Service (incluyendo `verify(..., never())` cuando la validación falla). No necesitan Docker ni PostgreSQL.

```bash
mvn test -Dtest="*ControllerTest"
```

Para ejecutar todas las pruebas del proyecto (incluidas las de Repository, que usan Testcontainers) se necesita Docker:

```bash
mvn clean test
```

### Estructura agregada

```
src/main/java/com/deepblue/rescue
├── controller   RescueCaseController, TreatmentController, AnimalController
├── dto
│   ├── request  ChangeRescueStatusRequest, CreateTreatmentRequest
│   └── response ErrorResponse, TreatmentEligibilityResponse, ...
└── exception    ResourceNotFoundException, BusinessRuleException, GlobalExceptionHandler
```