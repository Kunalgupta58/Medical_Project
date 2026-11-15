# ElderCare 360 deployment

The app is containerized and can run on any Docker-compatible host. For a no-cost
demo, host the static frontend on Vercel, the Spring Boot and OCR containers on
Render free web services, and PostgreSQL on a free managed database plan (for
example, Neon). This is one example combination, not a platform requirement.
Provider free-tier quotas and availability change; check current limits before
deploying.

> **Demo use only:** Free web services can sleep and their local filesystems are
> temporary. This app stores uploaded medical documents on the backend's local
> filesystem, so documents may be lost when an instance restarts or redeploys.
> Do not deploy real patient information or rely on this setup for care.
> Persistent private document storage is required for real use; this project
> currently supports local filesystem storage, not S3-compatible storage.

## Free-tier demo setup

The steps below describe provider settings rather than a provider-specific
blueprint, so the same Docker images can be used on other platforms.

### 1. PostgreSQL

Create a PostgreSQL database on a free managed database plan. Save its hostname,
database name, username, password, and port. The backend needs a JDBC URL:

```text
jdbc:postgresql://HOST:PORT/DATABASE?sslmode=require
```

Use the SSL option required by your provider. Keep credentials private. Flyway
creates and validates the app schema automatically when the backend starts.

### 2. OCR service

Create a Docker web service with `ocr-service` as its root directory and
`Dockerfile` as its Dockerfile. The image installs Tesseract and reads the
platform-provided `PORT` (default `8001`). Verify `https://YOUR_OCR/health`.
OCR can be slow on free instances and may time out while a sleeping service
wakes up.

### 3. Backend

Create a Docker web service with `backend` as its root directory and
`Dockerfile` as its Dockerfile. Configure:

| Variable | Value |
|---|---|
| `DATABASE_URL` | JDBC URL from step 1 |
| `DATABASE_USERNAME` | Database username |
| `DATABASE_PASSWORD` | Database password |
| `JWT_SECRET` | New random secret, at least 32 bytes |
| `FRONTEND_URL` | Exact deployed frontend origin, e.g. `https://my-app.vercel.app` |
| `OCR_SERVICE_URL` | OCR service URL from step 3 |
| `DOCUMENT_STORAGE_PATH` | `/tmp/medical-documents` for a disposable demo only |
| `EMAIL_ENABLED` | `false` unless SMTP is configured |

Most container platforms provide `PORT`; the Spring service listens on it. Wait
for the deployment and verify `https://YOUR_BACKEND/actuator/health`. Spring Boot
runs Flyway migrations at startup.

Generate a signing secret locally with:

```bash
openssl rand -base64 48
```

Never reuse the example value in `.env.example` or commit a real secret.

### 4. Frontend

Import the repository into Vercel and set the project root to `frontend`.
`frontend/vercel.json` handles React Router routes. Set these **build-time**
environment variables before deployment:

| Variable | Value |
|---|---|
| `VITE_API_URL` | Backend origin, e.g. `https://YOUR_BACKEND` (no trailing slash) |
| `VITE_OCR_URL` | OCR origin, e.g. `https://YOUR_OCR` |

For another static host, build with `npm ci && npm run build` in `frontend/`,
publish `frontend/dist`, configure SPA fallback to `index.html`, and set the
same Vite variables at build time.

After the frontend is live, set the backend's `FRONTEND_URL` to that exact
origin and redeploy the backend. Then test registration, login, API calls,
document upload/download, and OCR using synthetic files and data only.

## Docker Compose (local or Docker host)

Requirements: Docker Engine and Docker Compose v2.

```bash
cp .env.example .env
# Edit .env and set strong, unique POSTGRES_PASSWORD and JWT_SECRET values.
docker compose -f deployment/docker-compose.yml up --build -d
docker compose -f deployment/docker-compose.yml ps
curl http://localhost:8080/actuator/health
curl http://localhost:8001/health
```

The frontend is at `http://localhost`, backend at `http://localhost:8080`, and
OCR at `http://localhost:8001`. The compose file keeps PostgreSQL and uploaded
documents in named volumes. Do not use the example credentials on an internet-
accessible host.

Stop services with:

```bash
docker compose -f deployment/docker-compose.yml down
```

## Required configuration

| Variable | Used by | Notes |
|---|---|---|
| `DATABASE_URL` | Backend | Full JDBC URL, not a `postgresql://` URI |
| `DATABASE_USERNAME` | Backend | Database user |
| `DATABASE_PASSWORD` | Backend | Database password |
| `JWT_SECRET` | Backend | Required; use a random secret of at least 32 bytes |
| `FRONTEND_URL` | Backend | Exact browser origin allowed by CORS |
| `DOCUMENT_STORAGE_PATH` | Backend | Must be writable; use persistent private storage for durable documents |
| `OCR_SERVICE_URL` | Backend | OCR service origin |
| `VITE_API_URL` | Frontend build | Backend origin, without trailing slash |
| `VITE_OCR_URL` | Frontend build | OCR service origin |
| `PORT` | Backend/OCR | Usually assigned by the hosting platform |

Vite variables are embedded in the frontend at build time; changing them in a
hosting dashboard requires a new frontend build.

## Health checks and troubleshooting

| Service | Check |
|---|---|
| Backend | `GET /actuator/health` |
| OCR | `GET /health` |
| Frontend | Load `/` and a nested route such as `/login` |

- Backend startup/database errors: verify JDBC URL, credentials, SSL mode, and
  database availability.
- JWT startup errors: set `JWT_SECRET`; the app intentionally does not fall back
  to a known signing key.
- Browser CORS errors: set `FRONTEND_URL` to the frontend's exact origin and
  redeploy the backend.
- API requests to localhost: set `VITE_API_URL` in the frontend build
  environment and rebuild.
- OCR errors: verify the OCR service URL and allow time for free instances to
  wake up.
- Missing uploaded documents after restart: expected on ephemeral storage;
  configure a persistent private volume before relying on document retention.
