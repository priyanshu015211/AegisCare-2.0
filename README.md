# AegisCare 2.0 — Connected healthcare operations

AegisCare is a healthcare coordination prototype focused on connected patient journeys: assessment, emergency visibility, facility coordination and operational analytics. It is not a diagnostic device or a substitute for clinical judgement.

## Technology

- **Frontend:** TypeScript + Vite
- **Backend:** Java 21 + Spring Boot 3.3
- **Primary database:** MySQL 8 + Flyway migrations
- **Local fallback / fast tests:** file-backed H2 (development only)
- **Authentication:** JWT bearer tokens, BCrypt password hashing and role-based access control
- **Hosting target:** Vercel (frontend) + Render (Spring Boot API and private MySQL 8 service)

## Phase 1: production foundation

- Stateless JWT authentication and an account-aware token converter.
- `ADMIN`, `CLINICIAN` and `COORDINATOR` roles.
- Administrator-only hospital and staff account provisioning; there is no public registration.
- Hospital-level scoping for patient and triage records and derived dashboard, emergency, coordination and analytics views.
- New records retain hospital and creator identifiers.
- Flyway owns schema evolution; Hibernate uses `ddl-auto=validate`.
- MySQL 8 is the production persistence target. Production waits briefly for MySQL startup and then fails closed if it remains unavailable.
- File-backed H2 is development-only and never used in production.
- Demo data is disabled by default and must be explicitly enabled for local development.
- Frontend sign-in, session-only token storage, sign-out and automatic redirect after token expiry/invalidity.
- Configurable frontend API origin and explicit backend CORS origins.
- H2-backed security integration tests plus an optional real-MySQL Testcontainers integration test.

## Prerequisites

Install Java 21, Maven, Node.js 22 or newer, npm, and Docker Desktop (for local MySQL 8 or Testcontainers verification).

## Run locally with MySQL 8 (recommended)

1. Copy `database/.env.example` to the repository root as `.env` and replace both password placeholders with unique local-only values.
2. Start MySQL:

```powershell
docker compose up -d mysql
docker compose ps
```

Wait until the MySQL service reports `healthy`.

3. Open a PowerShell terminal in `backend` and configure the backend connection. Use the same app password set in the root `.env` file:

```powershell
$env:AEGISCARE_REQUIRE_MYSQL = "true"
$env:AEGISCARE_MYSQL_JDBC_URL = "jdbc:mysql://localhost:3306/aegiscare?connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true"
$env:AEGISCARE_MYSQL_USERNAME = "aegiscare_app"
$env:AEGISCARE_MYSQL_PASSWORD = "YOUR_LOCAL_MYSQL_APP_PASSWORD"

$bytes = New-Object byte[] 48
$rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$rng.GetBytes($bytes)
$env:AEGISCARE_JWT_SECRET = [Convert]::ToBase64String($bytes)
$rng.Dispose()

$env:AEGISCARE_DEMO_DATA = "true"
$env:AEGISCARE_BOOTSTRAP_ADMIN_ENABLED = "true"
$env:AEGISCARE_BOOTSTRAP_ADMIN_EMAIL = "admin@localhost.test"
$env:AEGISCARE_BOOTSTRAP_ADMIN_PASSWORD = "Choose-A-Unique-Password-2026!"
$env:AEGISCARE_BOOTSTRAP_ADMIN_NAME = "AegisCare Administrator"
mvn spring-boot:run
```

The first startup creates the bootstrap administrator if that email does not already exist. It never resets an existing password. Keep passwords and secrets out of source control.

### H2 fallback for quick local work

When MySQL/Docker is unavailable, leave the MySQL connection variables unset and set `AEGISCARE_REQUIRE_MYSQL=false`. The backend uses file-backed H2 under `backend/data/`. This mode is for development only; do not use it for a hosted deployment.

## Frontend

In another terminal:

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

See [backend/README.md](backend/README.md) for endpoint examples, schema details and environment configuration. See [SECURITY.md](SECURITY.md) for the authentication model, role boundaries, deployment-secret handling and remaining security work.

## Tests and package

Fast test suite (H2-backed, no Docker required):

```powershell
cd backend
mvn clean test
```

The MySQL 8 Testcontainers test runs automatically when Docker Desktop is available and is skipped when Docker is unavailable. It validates the MySQL-specific Flyway migrations, Hibernate schema validation, InnoDB table creation and UTC `Instant` persistence. To require the real-MySQL test locally, start Docker Desktop and run `mvn -Dtest=MySqlPersistenceIntegrationTest test` from `backend`.

Frontend checks:

```powershell
cd frontend
npm ci
npm audit
npm run build
```

## Deployment: Vercel + Render + MySQL 8

The root `render.yaml` defines two Render services:

- `aegiscare-mysql`: private MySQL 8 service with a persistent disk mounted at `/var/lib/mysql`.
- `aegiscare-backend`: Spring Boot Docker web service using the private MySQL endpoint and generated app credentials.

Create/sync the Render Blueprint from the repository root. Both services are configured for Singapore; keep them in the same Render workspace and region. The MySQL Dockerfile exposes port 3306 for private-service port detection, and its data disk is mounted at `/var/lib/mysql`. Wait for the MySQL private service to become live before relying on the backend deployment. The database is not publicly exposed. Render resolves the backend Dockerfile and build context relative to `rootDir: backend`.

Configure `AEGISCARE_CORS_ALLOWED_ORIGINS` to the exact Vercel origin. Configure the bootstrap administrator email/password in Render. After first successful account creation and login, set `AEGISCARE_BOOTSTRAP_ADMIN_ENABLED=false` and redeploy.

For Vercel, set `VITE_API_BASE_URL` to the Render backend origin (without `/api/v1`). Never commit database credentials, bootstrap passwords, JWT secrets or real `.env` files.

### Database operations

MySQL runs on a Render persistent disk, but disk snapshots are not a substitute for database-aware backups. Use `mysqldump --single-transaction`, encrypt the resulting dump, store it outside the database service, and test restore procedures. See [database/README.md](database/README.md).

Render-hosted MySQL requires a paid private service and persistent disk; it is not a free managed database. Review current Render pricing before provisioning.

## Important limitations

Phase 1 establishes an application-security baseline, not full production/clinical readiness. Before any real patient use, AegisCare still needs independent security review, legal/privacy review, tested encrypted backups and restore, audit-retention policy, operational monitoring, clinical governance of triage rules, accessibility testing and end-to-end deployment validation. The current rule-based risk score is a prototype signal only and must not drive clinical decisions.
