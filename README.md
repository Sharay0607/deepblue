# DeepBlue Rescue — Capa de persistencia

## 1. Descripción

Laboratorio de persistencia con **Java 21, Spring Boot 4, Spring Data JPA, Hibernate, Flyway,
PostgreSQL y Testcontainers**. Modela el dominio de **DeepBlue Rescue**, una plataforma para
centros de rescate y rehabilitación de fauna marina. Cubre exclusivamente la capa de
persistencia: no incluye controllers, servicios, DTOs ni seguridad.

## 2. Modelo de datos

```text
rescue_centers
rescue_cases
animals
medical_records
specialists
expertise
specialist_expertise   (tabla asociativa)
treatments
```

## 3. Relaciones

```text
RescueCenter   1 ────── N   RescueCase
RescueCase     1 ────── 1   Animal
Animal         1 ────── 1   MedicalRecord
Specialist     N ────── M   Expertise   (vía specialist_expertise)
Animal         1 ────── N   Treatment
Specialist     1 ────── N   Treatment
```

## 4. Ejecutar la aplicación

Requiere una instancia de PostgreSQL disponible (por ejemplo con Docker):

```bash
docker compose up -d
mvn spring-boot:run
```

## 5. Ejecutar las pruebas

Las pruebas usan **Testcontainers**: levantan su propio contenedor PostgreSQL, no requieren
Docker Compose ni una base de datos externa, solo Docker corriendo en la máquina.

```bash
mvn clean test
```

Resultado esperado: `BUILD SUCCESS`.

## 6. Flyway

`ddl-auto: validate` está configurado a propósito: Hibernate **nunca** crea ni modifica el
esquema, solo lo valida contra lo que ya existe. El esquema real lo crean y evolucionan las
migraciones versionadas de Flyway en `src/main/resources/db/migration`:

- `V1__create_schema.sql` — esquema completo (tablas, PK, FK, UNIQUE, CHECK, índices).
- `V2__insert_expertise_catalog.sql` — catálogo inicial de áreas de experiencia.
- `V3__add_tracking_device_to_animal.sql` — evolución del esquema: añade
  `tracking_device_code` a `animals` sin tocar `V1`.

## 7. Testcontainers

`PersistenceIntegrationTest` anota el contenedor con `@Container` y `@ServiceConnection`, lo
que hace que Spring Boot configure automáticamente el `DataSource` de las pruebas contra ese
PostgreSQL real y efímero, sin tocar `application.yml`. Así las pruebas validan constraints
reales (UNIQUE, FK, CHECK) que un H2 en memoria no reproduciría fielmente.

## 8. Query Methods implementados

- `RescueCenterRepository.findByCode`
- `RescueCaseRepository.findByCaseCode`
- `RescueCaseRepository.existsByCaseCode`
- `RescueCaseRepository.findByStatusOrderByRescueDateAsc`
- `RescueCaseRepository.findByRescueCenterCode`
- `RescueCaseRepository.findByRescueDateAfterOrderByRescueDateDesc`
- `AnimalRepository.findByAnimalCode`
- `AnimalRepository.findByCommonNameContainingIgnoreCase`
- `AnimalRepository.findByRescueCaseStatus`
- `AnimalRepository.findByRescueCaseRescueCenterCode`
- `ExpertiseRepository.findByNameIgnoreCase`
- `TreatmentRepository.findByAnimalIdOrderByPerformedAtAsc`

## 9. Consultas JPQL (`@Query`) implementadas

- `SpecialistRepository.findActiveByExpertise` — especialistas activos con determinada
  experiencia.
- `TreatmentRepository.findByPerformedAtBetween` — tratamientos en un intervalo de fechas.
- `TreatmentRepository.findByAnimalRescueCaseRescueCenterCode` — tratamientos de un centro,
  navegando `Treatment → Animal → RescueCase → RescueCenter`.
- `TreatmentRepository.findBySpecialistExpertise` — tratamientos por especialistas con
  determinada experiencia (N:M).
- `AnimalRepository.findInRehabilitationTreatedBySpecialistWithExpertise` — reto final:
  animales en rehabilitación tratados por un especialista con determinada experiencia.

## Verificación

Se ejecutó `mvn clean test` con todos los 16 tests de `PersistenceIntegrationTest`
(tanto de forma individual como la clase completa) obteniendo `BUILD SUCCESS`. Asi mismo, se probó el funcionamiento
de los test tanto corriendo la clase completa como usando maven for java.