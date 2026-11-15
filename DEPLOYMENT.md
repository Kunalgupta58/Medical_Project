# ElderCare 360 — Deployment Guide

## Architecture

| Component | Technology | Hosting |
|---|---|---|
| Frontend | React + Vite → Nginx | Vercel, Netlify, or Docker |
| Backend | Spring Boot 3.2 (JAR) | Render, Railway, or Docker |
| Database | PostgreSQL 16 | Neon, Supabase, or Docker |
| OCR Service | Python FastAPI | Railway, Fly.io, or Docker |
| File Storage | Local filesystem (Docker volume) | Persistent volume mount |

---

## Option A — Docker Compose (Recommended for local / VPS)

### Requirements
- Docker Engine 24+
- Docker Compose v2

### Steps

```bash
# 1. Clone
git clone https://github.com/YOUR_USERNAME/eldercare-360.git
cd eldercare-360

# 2. Create .env
cp .env.example .env
# Edit .env — set strong POSTGRES_PASSWORD and JWT_SECRET

# 3. Build and run
cd deployment
docker compose up --build -d

# 4. Check health
docker compose ps
curl http://localhost:8080/actuator/health
curl http://localhost:8001/health
```

Services will be available at:
- Frontend: http://localhost:80
- Backend: http://localhost:8080
- OCR: http://localhost:8001
- Swagger: http://localhost:8080/swagger-ui.html

### Stopping
```bash
docker compose down
# To also remove data volumes:
docker compose down -v
```

---

## Option B — Vercel (Frontend) + Render (Backend) + Neon (Database)

### Prerequisites

You need accounts on:
- [Vercel](https://vercel.com) (free tier)
- [Render](https://render.com) (free tier — note: spins down after inactivity)
- [Neon](https://neon.tech) (free tier PostgreSQL)

### Step 1: Database (Neon)

1. Create a Neon project
2. Copy the connection string: `postgresql://user:password@host/eldercare360`
3. Flyway will auto-run migrations on first backend startup

### Step 2: Backend (Render)

1. Connect your GitHub repository
2. Create a new **Web Service**
3. Set:
   - Build command: `cd backend && mvn package -DskipTests`
   - Start command: `java -jar backend/target/*.jar`
4. Add environment variables:

```
DATABASE_URL=postgresql://user:password@host/eldercare360
DATABASE_USERNAME=your_neon_user
DATABASE_PASSWORD=your_neon_password
JWT_SECRET=<generate: openssl rand -base64 64>
DOCUMENT_STORAGE_PATH=/opt/render/project/uploads
OCR_SERVICE_URL=https://your-ocr-service.railway.app
FRONTEND_URL=https://your-app.vercel.app
EMAIL_ENABLED=false
```

5. After deploy, note your URL: `https://your-backend.onrender.com`
6. Test: `curl https://your-backend.onrender.com/actuator/health`

### Step 3: OCR Service (Railway / Fly.io)

**Railway:**
1. Create new project → Deploy from GitHub (ocr-service folder)
2. Set `PORT=8001`
3. Railway will detect the Dockerfile

**Fly.io:**
```bash
cd ocr-service
fly launch
fly deploy
```

Note your OCR URL.

### Step 4: Frontend (Vercel)

1. Import repository to Vercel
2. Set root directory: `frontend`
3. Framework preset: Vite
4. Add environment variables:

```
VITE_API_URL=https://your-backend.onrender.com
VITE_OCR_URL=https://your-ocr.railway.app
```

5. Deploy

---

## Production Environment Variables

### Backend (required)
```
DATABASE_URL           # Full JDBC URL
DATABASE_USERNAME      # DB username
DATABASE_PASSWORD      # DB password
JWT_SECRET             # Min 256-bit random string
DOCUMENT_STORAGE_PATH  # Persistent path for medical files
FRONTEND_URL           # Your frontend URL (for CORS)
OCR_SERVICE_URL        # Your OCR service URL
```

### Backend (optional)
```
EMAIL_ENABLED=true          # Enable email notifications
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your@gmail.com
MAIL_PASSWORD=app_password   # Use Gmail App Password
PORT=8080                    # Server port
```

### Frontend
```
VITE_API_URL    # Backend URL (without trailing slash)
VITE_OCR_URL    # OCR service URL
```

---

## Database Migrations

Flyway runs automatically on backend startup.

Migrations are in: `backend/src/main/resources/db/migration/`

To run manually:
```bash
cd backend
mvn flyway:migrate -Dflyway.url=jdbc:postgresql://host:5432/eldercare360 \
  -Dflyway.user=postgres -Dflyway.password=yourpassword
```

---

## Document Storage

Medical documents must be stored in a **persistent** location.

- In Docker: use a named volume (`document_storage:/app/uploads/medical-documents`)
- On Render: use a Persistent Disk (requires paid plan) or an S3-compatible service
- **Never** store medical documents in ephemeral storage or as public GitHub assets

The backend serves documents only after JWT authentication check.

---

## Health Checks

| Service | Endpoint | Expected |
|---|---|---|
| Backend | `GET /actuator/health` | `{"status":"UP"}` |
| OCR | `GET /health` | `{"status":"ok"}` |
| Frontend | HTTP 200 on `/` | HTML response |

---

## Troubleshooting

| Problem | Solution |
|---|---|
| Backend fails to start | Check DATABASE_URL and credentials. Check Flyway migration logs. |
| JWT errors | Ensure JWT_SECRET is at least 32 characters |
| OCR returns error | Check Tesseract is installed: `tesseract --version` |
| File upload fails | Check DOCUMENT_STORAGE_PATH exists and is writable |
| CORS errors | Set FRONTEND_URL to match your exact frontend origin |
| Notifications not received | EMAIL_ENABLED=false → notifications are in-app only |

---

## Security Checklist for Production

- [ ] Change default JWT_SECRET
- [ ] Change default database password
- [ ] Enable HTTPS (required — use reverse proxy or hosting platform TLS)
- [ ] Set FRONTEND_URL to exact production origin
- [ ] Do not expose /actuator/env or /actuator/beans
- [ ] Use persistent volume for document storage
- [ ] Never commit .env file to Git
- [ ] Review CORS configuration

---

## Live URLs

> To be filled after deployment.

- Frontend: `https://[your-app].vercel.app`
- Backend health: `https://[your-backend].onrender.com/actuator/health`
- Swagger UI: `https://[your-backend].onrender.com/swagger-ui.html`
- OCR health: `https://[your-ocr].railway.app/health`

*Do not publish this file with invented URLs. Update with actual URLs after successful deployment.*
