# 📦 AI Warehouse Package Quality Inspection System
## Technical Architecture & Detailed Implementation Document

---

## 1. Executive Summary & Core Objectives

The **AI Warehouse Package Quality Inspection System** is an enterprise-grade Java Spring Boot application designed to automate damage detection, risk classification, and incident management in logistics and warehouse workflows.

Key architectural features include:
- **In-Process Convolutional Neural Network (CNN) Inference**: Powered by **Deep Java Library (DJL)** with native PyTorch C++ engine integration inside the Java Virtual Machine (JVM).
- **Graceful Fallback Mechanism**: Seamless fallback to a demonstration analyzer if native model weights are not present.
- **Automated Risk & Severity Engine**: Heuristic engine calculating incident severity (`LOW`, `MEDIUM`, `HIGH`) from CNN classification confidence and damage categories.
- **Incident State Machine**: Workflow tracking (`OPEN`, `NEEDS_REVIEW`, `ASSIGNED`, `IN_PROGRESS`, `ESCALATED`, `RESOLVED`, `REJECTED`).
- **Audit History Log**: Immutable, append-only audit trail logging all lifecycle operations.
- **Interactive UI Dashboard**: Modern Web interface built with Bootstrap 5 and JavaScript.

---

## 2. Deep Java Library (DJL) CNN Engine Architecture

### 2.1 Why In-JVM Machine Learning?
Traditional enterprise Java applications rely on external Python microservices (FastAPI/Flask/TorchServe) connected over REST/gRPC for ML model inference. While common, this architecture introduces network latency, serialization overhead, complex operational deployments, and multiple points of failure.

**Deep Java Library (DJL)** provides high-level Java abstractions built on top of native C++ engines (PyTorch, ONNX Runtime, TensorFlow). Through Java Native Interface (JNI), DJL executes model tensors directly in C++ memory spaces while keeping memory control and API management inside Java.

### 2.2 Inference Execution Pipeline (`DJLImageClassificationService.java`)

```text
 ┌────────────────┐
 │ Image File     │
 └───────┬────────┘
         │ (InputStream)
         ▼
 ┌────────────────────────────────────────────────────────┐
 │ DJL ImageFactory                                       │
 │ Converts raw byte array into DJL Image object          │
 └───────┬────────────────────────────────────────────────┘
         │
         ▼
 ┌────────────────────────────────────────────────────────┐
 │ DJL Preprocessing Pipeline                             │
 │ 1. Resize(224, 224)                                    │
 │ 2. ToTensor() -> Scales pixel values [0, 255] -> [0,1] │
 │ 3. Normalize(mean, std) -> ImageNet standard           │
 └───────┬────────────────────────────────────────────────┘
         │
         ▼
 ┌────────────────────────────────────────────────────────┐
 │ DJL PyTorch Predictor                                  │
 │ Executes forward pass through PyTorch TorchScript model│
 └───────┬────────────────────────────────────────────────┘
         │
         ▼
 ┌────────────────────────────────────────────────────────┐
 │ Postprocessing & Classification Result                 │
 │ Translates output tensor into DamageType & Confidence  │
 └────────────────────────────────────────────────────────┘
```

### 2.3 Resilient Fallback Strategy
If DJL fails to load native C++ binaries (e.g. system architecture mismatch) or if the model file `package-quality-model.pt` is absent from `models/`, `DJLImageClassificationService`:
1. Catches initialization exceptions without interrupting Spring Boot application startup.
2. Sets an internal flag `useFallback = true`.
3. Seamlessly routes inference calls to `MockImageClassificationService`, ensuring zero system downtime during local development or test environments.

---

## 3. Storage & Severity Calculation Services

### 3.1 File Storage Engine (`FileStorageService.java`)
- **Format Validation**: Accepts only valid image extensions (`.jpg`, `.jpeg`, `.png`, `.webp`).
- **Unique Naming**: Generates collision-resistant filenames using `UUID.randomUUID()` and epoch timestamps.
- **Resource Handler**: Maps physical `uploads/` directory to `/uploads/**` static URL path via Spring Web MVC (`WebConfig.java`).

### 3.2 Severity & Risk Matrix (`SeverityCalculationService.java`)
Risk severity is assigned deterministically based on damage class and CNN prediction confidence:

| Damage Class | Confidence | Assigned Severity | Initial Status |
| :--- | :--- | :--- | :--- |
| `PUNCTURE` / `CRUSHED` | $\ge 85\%$ | `HIGH` | `OPEN` |
| `PUNCTURE` / `CRUSHED` | $< 85\%$ | `MEDIUM` | `NEEDS_REVIEW` |
| `DENT` / `TEAR` | $\ge 85\%$ | `MEDIUM` | `OPEN` |
| `DENT` / `TEAR` | $< 85\%$ | `LOW` | `NEEDS_REVIEW` |
| `NO_DAMAGE` | Any | `LOW` | `OPEN` |

---

## 4. Incident State Machine & Domain Entities

### 4.1 Lifecycle State Transitions

```text
               ┌──────────────┐
               │ NEEDS_REVIEW │
               └──────┬───────┘
                      │ (Confidence < 75%)
                      ▼
  [Upload] ─────►   OPEN   ─────► ASSIGNED ─────► IN_PROGRESS ─────► RESOLVED
                      │              │                 │                ▲
                      │              ▼                 ▼                │
                      └──────────► ESCALATED ───────────────────────────┘
                                     │
                                     ▼
                                  REJECTED
```

### 4.2 Entity Data Model (`JPA Entities`)
- **`User`**: Core user record (`id`, `name`, `email`, `role`).
- **`Inspection`**: Central domain record containing `image_path`, `predicted_class`, `confidence`, `severity`, `status`, `assigned_to`, and timestamps.
- **`AuditLog`**: Append-only log table capturing `inspection_id`, `action`, `performed_by`, `timestamp`, and detailed change descriptions.

---

## 5. Summary of REST APIs

| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `POST` | `/api/inspections` | Upload image for CNN analysis & create record |
| `GET` | `/api/inspections` | Query inspection registry with optional filters |
| `GET` | `/api/inspections/{id}` | Get detailed inspection record |
| `PUT` | `/api/inspections/{id}/assign/{userId}` | Assign inspection to inspector |
| `PUT` | `/api/inspections/{id}/escalate` | Escalate incident & mark HIGH severity |
| `PUT` | `/api/inspections/{id}/resolve` | Mark inspection as RESOLVED |
| `GET` | `/api/inspections/{id}/history` | Fetch complete audit timeline |
| `GET` | `/api/dashboard/stats` | Fetch aggregate metrics & damage distribution |
| `GET` | `/api/users` | List registered inspectors and supervisors |

