# ElderCare 360 — Intelligent Elderly Care & Medical Record Management Platform

> **PromptWars × Catalyst Hackathon — Elderly Care & Medication Management Challenge**

[![Live Demo](https://img.shields.io/badge/Live-Demo-blue)](http://localhost)
[![Backend API](https://img.shields.io/badge/API-Swagger-green)](http://localhost:8080/swagger-ui.html)
[![License](https://img.shields.io/badge/License-MIT-gray)](./LICENSE)

---

## Overview

ElderCare 360 is a full-stack web platform that connects elderly users, family members and caregivers on one secure platform for:

- **Medication scheduling** with one-tap dose confirmation
- **Caregiver escalation** when doses are unconfirmed
- **Secure Medical Vault** with private document storage
- **Local Tesseract OCR** for prescription text extraction (no external AI APIs)
- **Family coordination** via invitation-based access control
- **Appointment management** with reminders
- **Emergency SOS** with instant caregiver notifications
- **Real-time dashboard** backed by actual PostgreSQL records

---

## Problem Statement

Elderly people commonly struggle with:
1. Forgetting scheduled medicines
2. Family members not knowing whether a dose was taken
3. Medical prescriptions scattered in physical files
4. Missing doctor appointments
5. Delayed emergency response

ElderCare 360 solves each of these with specific, working features — not mockups.

---

## Technology Stack

| Layer | Technology |
|---|---|
| Frontend | React 18, Vite, Tailwind CSS, Recharts, React Router |
| Backend | Java 21, Spring Boot 3.2, Spring Security, JWT, JPA |
| Database | PostgreSQL 16, Flyway migrations |
| OCR | Python 3.11, FastAPI, Tesseract OCR, OpenCV, PyMuPDF |
| Auth | BCrypt passwords, JWT tokens, Role-based access control |
| Deployment | Docker, Docker Compose |

---

## Architecture

```
React Frontend (Vite + Tailwind)
        │
   REST API (HTTPS)
        │
Spring Boot Backend (Java 21)
        │
 ┌──────┼──────────────┬──────────────┐
 │      │              │              │
Auth  Medication   Medical Vault  Appointments
      Scheduler    File Storage   Notifications
         │              │
   Escalation    Python OCR Service
         │              │
       PostgreSQL ◄──── Flyway
```

---

## Features

| Feature | Status |
|---|---|
| User registration and login (JWT) | ✅ |
| Role-based access (Elderly, Family, Caregiver, Admin) | ✅ |
| Medication CRUD with schedule generation | ✅ |
| Today's dose schedule with Take/Snooze/Skip | ✅ |
| Caregiver escalation for unconfirmed doses | ✅ |
| Medical Vault — upload PDF/JPG/PNG | ✅ |
| Private file storage with SHA-256 verification | ✅ |
| Local Tesseract OCR for prescriptions | ✅ |
| OCR candidate field extraction with uncertainty flags | ✅ |
| Medical history timeline | ✅ |
| Family invitation system | ✅ |
| Appointment management | ✅ |
| In-app notification center | ✅ |
| Emergency SOS with demo mode | ✅ |
| Dashboard with Recharts (real data) | ✅ |
| Docker Compose deployment | ✅ |

---

## Quick Start (Local Development)

### Prerequisites

- Java 21+
- Maven 3.8+
- Node.js 20+
- PostgreSQL 14+
- Python 3.11+ (for OCR)
- Tesseract OCR installed (`tesseract --version`)

### 1. Clone

```bash
git clone https://github.com/YOUR_USERNAME/eldercare-360.git
cd eldercare-360
```

### 2. Configure environment

```bash
cp .env.example .env
# Edit .env with your database credentials and JWT secret
```

### 3. Run with Docker Compose

```bash
docker compose -f deployment/docker-compose.yml up --build
```

The application will be available at:
- Frontend: http://localhost
- Backend API: http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui.html
- OCR Service: http://localhost:8001

### 4. Manual setup (without Docker)

**Database:**
```bash
psql -U postgres -c "CREATE DATABASE eldercare360;"
```

**Backend:**
```bash
cd backend
export JWT_SECRET="$(openssl rand -base64 48)"
mvn spring-boot:run
```

**OCR Service:**
```bash
cd ocr-service
pip install -r requirements.txt
uvicorn app.main:app --port 8001 --reload
```

**Frontend:**
```bash
cd frontend
npm install
npm run dev
```

---

## Demo Scenario (Synthetic Data)

The platform uses clearly labelled synthetic data for demonstration.

| User | Role | Description |
|---|---|---|
| Ramesh Sharma | ELDERLY | Primary elderly user |
| Priya Sharma | FAMILY_MEMBER | Daughter — connected via invitation |
| Amit Kumar | CAREGIVER | Professional caregiver |

**Demo flow:**
1. Register as Ramesh Sharma (ELDERLY)
2. Add medication: Metformin 500mg, Twice daily, 08:00 and 20:00
3. Open Today's Schedule → confirm morning dose as taken
4. Leave evening dose unconfirmed → caregiver escalation triggers in 30 minutes
5. Upload a synthetic prescription → run local OCR → verify candidate fields
6. Create a doctor appointment
7. View dashboard with actual dose statistics
8. Trigger demo SOS → see caregiver notification

---

## Environment Variables

See `.env.example` for all required variables.

Copy `.env.example` to `.env` before running Docker Compose. `JWT_SECRET` must be set; the application intentionally has no built-in production signing key.

Key variables:

| Variable | Description |
|---|---|
| `DATABASE_URL` | PostgreSQL JDBC URL |
| `JWT_SECRET` | At least 256-bit random string |
| `DOCUMENT_STORAGE_PATH` | Private file storage path |
| `OCR_SERVICE_URL` | URL of the OCR FastAPI service |
| `EMAIL_ENABLED` | Set `true` to enable email notifications |

---

## Running Tests

**Backend:**
```bash
cd backend && mvn test
```

**OCR Service:**
```bash
cd ocr-service && pip install -r requirements-dev.txt && pytest tests/ -v
```

**Frontend:**
```bash
cd frontend && npm test
```

---

## API Documentation

Swagger UI is available at `/swagger-ui.html` when the backend is running.

Base path: `/api/v1`

Key endpoints:
- `POST /api/v1/auth/register`
- `POST /api/v1/auth/login`
- `GET  /api/v1/medication-schedules/today`
- `POST /api/v1/doses/{id}/taken`
- `POST /api/v1/medical-documents/upload`
- `GET  /api/v1/dashboard/overview`
- `POST /api/v1/emergency/sos`

---

## Security

- Passwords hashed with BCrypt (cost factor 12)
- JWT tokens for stateless authentication
- Role-based and object-level authorization
- Medical documents stored in private directory (never publicly accessible)
- File type and size validation on upload
- SQL injection protection via JPA/Hibernate parameterized queries
- CORS restricted to configured frontend origins

---

## Known Limitations

1. **Email delivery** requires SMTP configuration (`EMAIL_ENABLED=true`). Without it, notifications are stored in-app only.
2. **SMS/WhatsApp** delivery is not implemented. Emergency contacts are notified via in-app notifications only.
3. **OCR accuracy** depends on Tesseract and scan quality. All extracted fields require manual verification.
4. **OCR service** requires Tesseract to be installed on the host or Docker container.
5. **This is a hackathon demonstration.** Use synthetic data only. Do not store real patient health information. Free hosting plans may use ephemeral storage and sleep when idle; uploaded documents can be lost.

---

## License

MIT License — see [LICENSE](./LICENSE)

---

## Hackathon

Built for **PromptWars × Catalyst — Elderly Care & Medication Management Challenge**

*Using synthetic patient data only. No real medical records were used.*
