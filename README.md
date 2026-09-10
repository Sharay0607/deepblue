# DeepBlue Rescue — Capa de persistencia

## 1\. Descripción

Laboratorio de persistencia con **Java 21, Spring Boot 4, Spring Data JPA, Hibernate, Flyway,
PostgreSQL y Testcontainers**. Modela el dominio de **DeepBlue Rescue**, una plataforma para
centros de rescate y rehabilitación de fauna marina. Cubre exclusivamente la capa de
persistencia: no incluye controllers, servicios, DTOs ni seguridad.

## 2\. Modelo de datos

```text
rescue\_centers
rescue\_cases
animals
medical\_records
specialists
expertise
specialist\_expertise   (tabla asociativa)
treatments
```

## 3\. Relaciones

```text
RescueCenter   1 ────── N   RescueCase
RescueCase     1 ────── 1   Animal
Animal         1 ────── 1   MedicalRecord
Specialist     N ────── M   Expertise   (vía specialist\_expertise)
Animal         1 ────── N   Treatment
Specialist     1 ────── N   Treatment
```

## 4\. Ejecutar la aplicación

Requiere una instancia de PostgreSQL disponible (por ejemplo con Docker):

```bash
docker compose up -d
mvn spring-boot:run
```

## 

