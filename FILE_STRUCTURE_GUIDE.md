# 📦 Complete File-by-File Technical Guide
## AI Warehouse Package Quality Inspection System

---

## 1. Maven Project Configuration

### `pom.xml`
- **Purpose**: Defines the Java project configuration, Spring Boot parent dependencies, library dependencies, build plugins, and Java version (Java 17).
- **Key Dependencies**:
  - `spring-boot-starter-web`: Provides REST API support, embedded Tomcat web server, and Spring MVC controllers.
  - `spring-boot-starter-data-jpa`: Object-Relational Mapping (ORM) using Hibernate to interact with PostgreSQL / H2 databases.
  - `spring-boot-starter-validation`: Jakarta Bean Validation (`@NotNull`, `@NotBlank`, `@Size`) for input validation.
  - `org.postgresql:postgresql`: PostgreSQL JDBC driver for production & dev database connections.
  - `com.h2database:h2`: Fast, in-memory SQL database used during JUnit test execution.
  - `org.flywaydb:flyway-core`: Database migration tool that automatically executes SQL schema scripts on application startup.
  - `ai.djl:api` & `ai.djl.pytorch:pytorch-engine`: **Deep Java Library (DJL)** core API and PyTorch engine bindings. Allows loading TorchScript PyTorch models directly inside JVM memory without external Python processes.
  - `org.projectlombok:lombok`: Code generator annotations (`@Data`, `@Getter`, `@Setter`, `@Builder`, `@NoArgsConstructor`, `@AllArgsConstructor`).

---

## 2. Environment & Application Configuration

### `src/main/resources/application.properties`
- **Purpose**: Base Spring Boot configuration file loaded by default.
- **Key Properties**:
  - `spring.profiles.active=dev`: Activates the `dev` profile by default.
  - `app.upload.dir=uploads/`: Base directory path where uploaded package images are stored on disk.
  - `app.ml.model-path=models/package-quality-model.pt`: File path where the PyTorch TorchScript CNN model binary is expected.

### `src/main/resources/application-dev.properties`
- **Purpose**: Configuration for local development environment.
- **Key Properties**:
  - Configures PostgreSQL connection parameters (`jdbc:postgresql://localhost:5432/warehouse_inspection_db`).
  - Sets `spring.jpa.hibernate.ddl-auto=validate`: Hibernate validates existing database schema against Java JPA entities without destroying database data.
  - `spring.flyway.enabled=true`: Enables Flyway database script migrations on app start.

### `src/main/resources/application-prod.properties`
- **Purpose**: Production configuration using system environment variables (`${DB_URL}`, `${DB_USERNAME}`, `${DB_PASSWORD}`) for security.

### `src/test/resources/application-test.properties`
- **Purpose**: Special profile used during JUnit test execution. Uses an in-memory H2 database (`jdbc:h2:mem:testdb`) so tests run isolated without requiring a local PostgreSQL database server.

---

## 3. Database Schema Migration Scripts (Flyway)

### `src/main/resources/db/migration/V1__initial_schema.sql`
- **Purpose**: Initial SQL migration script executed by Flyway when database starts up.
- **Tables Created**:
  - `users`: Stores system users with columns `id`, `name`, `email` (UNIQUE), `role` (`ADMIN`, `INSPECTOR`, `SUPERVISOR`), and `created_at`.
  - `inspections`: Primary table storing package inspection entries (`id`, `image_path`, `predicted_class`, `confidence`, `severity`, `status`, `description`, `assigned_to` FK to `users`, `created_at`, `updated_at`, `resolved_at`).
  - `audit_logs`: Immutable audit trail table (`id`, `inspection_id` FK to `inspections`, `action`, `performed_by`, `timestamp`, `description`).
- **Indexes**: Creates database performance indexes on `status`, `severity`, `predicted_class`, and `assigned_to` columns.

### `src/main/resources/db/migration/V2__sample_data.sql`
- **Purpose**: Seeds initial seed data (default warehouse inspectors, supervisors, and mock inspections) into the database for immediate testing.

---

## 4. Domain Enums & JPA Entities

### `src/main/java/com/example/packageinspection/entity/DamageType.java`
- **Values**: `DENT`, `TEAR`, `PUNCTURE`, `CRUSHED`, `NO_DAMAGE`
- **Purpose**: Enumeration of package damage classes predicted by the CNN deep learning model.

### `src/main/java/com/example/packageinspection/entity/Severity.java`
- **Values**: `LOW`, `MEDIUM`, `HIGH`
- **Purpose**: Enumeration representing risk severity level calculated by the severity engine.

### `src/main/java/com/example/packageinspection/entity/InspectionStatus.java`
- **Values**: `OPEN`, `NEEDS_REVIEW`, `ASSIGNED`, `IN_PROGRESS`, `ESCALATED`, `RESOLVED`, `REJECTED`
- **Purpose**: Lifecycle workflow statuses for package quality incidents.

### `src/main/java/com/example/packageinspection/entity/Role.java` & `AuditAction.java`
- **Purpose**: System user roles (`ADMIN`, `INSPECTOR`, `SUPERVISOR`) and audit action types (`CREATED`, `ASSIGNED`, `STATUS_UPDATED`, `ESCALATED`, `RESOLVED`, `REJECTED`).

### Entities (`User.java`, `Inspection.java`, `AuditLog.java`)
- **Purpose**: JPA ORM Entities mapping Java objects directly to relational PostgreSQL tables.

---

## 5. Repository Layer (Spring Data JPA)

### `src/main/java/com/example/packageinspection/repository/UserRepository.java`
- **Purpose**: Database interface for `User` entity extending `JpaRepository<User, Long>`.

### `src/main/java/com/example/packageinspection/repository/InspectionRepository.java`
- **Purpose**: Database interface for `Inspection` entity with custom aggregation queries (`findAverageConfidence()`, `countGroupByPredictedClass()`, `findByFilters(...)`).

### `src/main/java/com/example/packageinspection/repository/AuditLogRepository.java`
- **Purpose**: Database interface for `AuditLog` entity to retrieve timeline history.

---

## 6. Machine Learning & DJL Layer

### `src/main/java/com/example/packageinspection/ml/ClassificationResult.java`
- **Purpose**: DTO holding CNN outputs: `predictedClass`, `confidence`, `isMockModel`, and diagnostic logs.

### `src/main/java/com/example/packageinspection/ml/ImageClassificationService.java`
- **Purpose**: Interface for image classification.

### `src/main/java/com/example/packageinspection/ml/DJLImageClassificationService.java`
- **Purpose**: Core ML Service using **Deep Java Library (DJL)**. Loads PyTorch model `package-quality-model.pt`, applies `Resize(224, 224)` & normalization pipeline, executes forward pass directly in JVM, and falls back to mock service if model is absent.

### `src/main/java/com/example/packageinspection/ml/MockImageClassificationService.java`
- **Purpose**: Mock implementation providing realistic classifications when no PyTorch model file is installed.

---

## 7. Service Layer & DTOs

### `FileStorageService.java`
- **Purpose**: Saves uploaded image bytes securely to disk (`uploads/`) with UUID filenames.

### `SeverityCalculationService.java`
- **Purpose**: Calculates risk severity (`LOW`, `MEDIUM`, `HIGH`) and initial status.

### `InspectionService.java`
- **Purpose**: Core domain service handling inspection creation, user assignment, escalation, and resolution while persisting audit log events.

### `AuditLogService.java`, `UserService.java`, `DashboardStatsService.java`
- **Purpose**: Support services handling audit writing, user query logic, and aggregate metrics.

---

## 8. Web & REST Controllers

### `InspectionController.java`
- **Endpoints**: `/api/inspections` (Upload, List/Filter, Get by ID, Assign, Escalate, Resolve, History).

### `DashboardController.java` & `UserController.java`
- **Endpoints**: `/api/dashboard/stats` and `/api/users`.

### `GlobalExceptionHandler.java` & `WebConfig.java`
- **Purpose**: Exception handling (`@ControllerAdvice`) returning JSON error bodies and static resource mapping for `/uploads/**`.

---

## 9. Frontend & Tests

### `src/main/resources/static/index.html` & `app.js`
- **Purpose**: Web Dashboard UI for file uploading, analytics visualization, inspection filtering, and timeline inspection modal.

### JUnit 5 & Mockito Tests (`src/test/java/...`)
- Unit tests verifying severity rules, storage validation, DJL ML fallback, and inspection service workflow.


