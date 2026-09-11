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