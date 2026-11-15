# ElderCare 360 — Architecture

## System Overview

```
┌─────────────────────────────────────────────────────┐
│                   ELDERCARE 360                      │
├─────────────────────────────────────────────────────┤
│                                                      │
│  React Frontend (Vite + Tailwind CSS)                │
│  ├── AuthContext (JWT storage)                       │
│  ├── API Layer (Axios)                               │
│  └── Pages: Dashboard, Meds, Vault, SOS...           │
│                            │                         │
│                      REST / HTTPS                    │
│                            │                         │
│  Spring Boot Backend (Java 21)                       │
│  ├── JwtAuthenticationFilter                         │
│  ├── Controllers (AuthController, MedController...)  │
│  ├── Services (MedicationService, VaultService...)   │
│  ├── MedicationReminderScheduler (cron + polling)    │
│  ├── DocumentStorageService (local filesystem)       │
│  └── NotificationService (in-app + optional email)   │
│                            │                         │
│  ┌─────────────────────────┼──────────────────────┐  │
│  │      PostgreSQL 16      │  OCR FastAPI Service  │  │
│  │  (Flyway migrations)    │  (Tesseract + OpenCV) │  │
│  │                         │  POST /ocr/process    │  │
│  │  Tables:                │                       │  │
│  │  - users                │  Pipeline:            │  │
│  │  - elderly_profiles     │  Upload → Preprocess  │  │
│  │  - medications          │  → Tesseract → Fields │  │
│  │  - medication_schedules │  → Uncertainty flags  │  │
│  │  - medical_documents    │  → Manual verify      │  │
│  │  - appointments         │                       │  │
│  │  - notifications        │  No external AI APIs  │  │
│  │  - care_tasks           │                       │  │
│  │  - emergency_events     │                       │  │
│  └─────────────────────────┴──────────────────────┘  │
└─────────────────────────────────────────────────────┘
```

## Data Flow — Medication Reminder

```
[Scheduler: every 5 min]
       │
       ▼
Find PENDING doses in next 5 minutes
       │
       ▼
Create in-app notification for elderly user
       │
       ▼
User confirms → status = TAKEN
       │
[No confirmation after 30 min]
       │
       ▼
status = UNCONFIRMED
Create escalation notification for all caregivers/family
       │
       ▼
Caregiver acknowledges → escalation_acknowledged = true
```

## Data Flow — Medical Document Upload

```
User selects file (PDF/JPG/PNG, max 20MB)
       │
       ▼
Client-side validation (size, extension)
       │
       ▼
POST /api/v1/medical-documents/upload (multipart)
       │
       ▼
Backend: validate file type (extension + content-type)
       │
       ▼
Compute SHA-256 checksum
       │
       ▼
Store file → /uploads/medical-documents/{elderlyId}/{uuid}.{ext}
(Never publicly accessible)
       │
       ▼
Save metadata to PostgreSQL
       │
       ▼
Return DocumentResponse (no raw file URL)
```

## Security Model

- Every API endpoint (except `/auth/**`) requires a valid JWT
- Object-level authorization: users can only access data belonging to their linked profiles
- Elderly users own their data; caregivers/family access is controlled by `caregiver_relationships`
- Medical documents are served only via authenticated download endpoint
- Storage keys are random UUIDs — no path guessing possible
- File contents are validated (extension + MIME type)
- Path traversal prevented by normalizing storage key against base path

## Database Schema Summary

See `database/migrations/` for the full Flyway migration.

Key relationships:
- `users` ← 1:1 → `elderly_profiles` (for ELDERLY role)
- `users` ←M:N→ `users` via `caregiver_relationships`
- `medications` → `medication_schedules` (generated daily by scheduler)
- `medical_documents` → stored in private filesystem, metadata in DB
- `notifications` → linked to users, persisted for history

## Technology Decisions

| Decision | Rationale |
|---|---|
| PostgreSQL over MySQL | JSONB support for flexible metadata, better Flyway support |
| Flyway over Liquibase | Simpler for single-schema apps, SQL-native |
| JWT (stateless) | Scales horizontally without session state |
| BCrypt cost 12 | Balances security and login latency |
| Tesseract over cloud OCR | No external API dependency, no data leaving the server |
| Local file storage | Avoid cloud storage costs for hackathon; production would use S3/GCS |
| In-app notifications first | SMS/email requires paid provider — in-app always works |
