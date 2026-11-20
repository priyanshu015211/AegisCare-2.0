# AegisCare 2.0 — Connected healthcare operations

AegisCare is a healthcare coordination prototype focused on connected patient journeys: assessment, emergency visibility, facility coordination and operational analytics. It is not a diagnostic device or a substitute for clinical judgement.

## Technology

- **Frontend:** TypeScript + Vite
- **Backend:** Java 21 + Spring Boot 3.3
- **Persistence:** PostgreSQL (Supabase) with Flyway migrations; file-backed H2 is development-only
- **Authentication:** JWT bearer tokens, BCrypt password hashing and role-based access control

## Phase 1: production foundation

This repository includes the first security and deployment-foundation pass:

- Stateless JWT authentication and an account-aware token converter.
- `ADMIN`, `CLINICIAN` and `COORDINATOR` roles.
- Administrator-only hospital and staff account provisioning; there is no public registration.
- Hospital-level scoping for patient and triage records and the derived dashboard, emergency, coordination and analytics views.
- New records retain hospital and creator identifiers.
- Flyway migrations; Hibernate schema mutation is disabled (`ddl-auto: validate`).
- Production can be configured to fail closed instead of falling back to local H2.
- Demo data is disabled by default and must be explicitly enabled for local development.
- Frontend sign-in, session-only token storage, sign-out and automatic redirect after token expiry/invalidity.
- Configurable frontend API origin and explicit backend CORS origins.
- Integration tests for login, protected routes and role enforcement.

## Local setup

Install Java 21, Maven, Node.js 22 or newer, and npm.

### Backend (PowerShell)

```powershell
cd backend
$bytes = New-Object byte[] 48
$rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$rng.GetBytes($bytes)
$env:AEGISCARE_JWT_SECRET = [Convert]::ToBase64String($bytes)
$rng.Dispose()
$env:AEGISCARE_DEMO_DATA = "true"
$env:AEGISCARE_REQUIRE_POSTGRES = "false"
$env:AEGISCARE_BOOTSTRAP_ADMIN_ENABLED = "true"
$env:AEGISCARE_BOOTSTRAP_ADMIN_EMAIL = "admin@localhost.test"
$env:AEGISCARE_BOOTSTRAP_ADMIN_PASSWORD = "Choose-A-Unique-Password-2026!"
$env:AEGISCARE_BOOTSTRAP_ADMIN_NAME = "AegisCare Administrator"
mvn spring-boot:run
```

The first startup creates the bootstrap administrator if that email does not already exist. It never resets an existing password. Keep these environment values out of source control. Local H2 data is stored under `backend/data/`.

### Frontend

```powershell
cd frontend
npm ci
npm run build
npm run dev
```

Open `http://localhost:5173`. The local Vite proxy forwards `/api/*` to `http://localhost:8080`. The landing page is public; care-team workspace routes require sign-in.

## Admin onboarding

1. Sign in using the bootstrap administrator.
2. Create a hospital with `POST /api/v1/admin/hospitals`.
3. Create clinician and coordinator accounts with `POST /api/v1/admin/users`, assigning each a hospital ID.
4. Sign out and sign in with the provisioned staff account to verify role restrictions and hospital scoping.

See [backend/README.md](backend/README.md) for endpoint examples, schema details and environment configuration.
See [SECURITY.md](SECURITY.md) for the authentication model, role boundaries, deployment-secret handling and remaining security work.

## Tests and package

```powershell
cd backend
mvn test
mvn clean package
```

```powershell
cd frontend
npm ci
npm run build
```

## Deployment notes

For Render, set `AEGISCARE_REQUIRE_POSTGRES=true`, `AEGISCARE_DEMO_DATA=false`, a unique `AEGISCARE_JWT_SECRET`, Supabase JDBC credentials, the initial admin bootstrap values and `AEGISCARE_CORS_ALLOWED_ORIGINS` to the exact Vercel origin. Disable bootstrap creation after the first administrator is created.

For Vercel, set `VITE_API_BASE_URL` to the Render backend origin (without `/api/v1`). Set the matching Vercel site origin in Render's CORS allowlist. Never commit real secrets or use H2 for production.

## Important limitations

Phase 1 establishes a security baseline, not full production/clinical readiness. Before any real patient use, AegisCare still needs security review, legal/privacy review, managed backup and restore testing, audit-retention policy, operational monitoring, clinical governance of triage rules, accessibility testing and end-to-end validation of the complete deployment. The current rule-based risk score is a prototype signal only and must not drive clinical decisions.
