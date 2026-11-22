# AegisCare backend

Java 21 / Spring Boot REST API for hospital operations, triage recording and care coordination.

## Phase 1 foundation

- Stateless JWT bearer authentication (`POST /api/v1/auth/login`).
- Roles: `ADMIN`, `CLINICIAN`, `COORDINATOR`.
- Administrator-only account and hospital provisioning endpoints.
- Hospital-level access scoping for patients, triage sessions, emergency data, dashboards, analytics, doctors and appointment summaries.
- BCrypt password hashing (work factor 12); no public self-registration.
- Account enabled/role state is checked against the database on each authenticated request, so disabled accounts lose access immediately.
- MySQL 8 is the primary production datastore; Flyway owns schema migrations and Hibernate runs in `validate` mode.
- File-backed H2 remains a development-only fallback. Production sets `AEGISCARE_REQUIRE_MYSQL=true` and fails closed if MySQL is missing or unreachable.
- Synthetic demo records are disabled by default. Enable them only in local development.

This is an application security foundation, not a clinical certification. Do not enter real patient data until deployment, privacy controls, backup/restore, audit requirements and clinical workflows have undergone appropriate review.

## Run locally with MySQL 8 (PowerShell)

From the repository root, copy `database/.env.example` to `.env`, replace the two password placeholders, and start the local database:

```powershell
docker compose up -d mysql
docker compose ps
```

Wait until MySQL reports `healthy`. Then open a PowerShell terminal in the `backend` directory:

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

The API listens on `http://localhost:8080`. The bootstrap admin is created only when the configured email does not already exist; it never resets an existing password. Keep all passwords and secrets out of source control.

### Development-only H2 fallback

To run without MySQL, leave the MySQL connection variables unset and set `AEGISCARE_REQUIRE_MYSQL=false`. The application uses file-backed H2 under `backend/data/`. Do not use this fallback in a hosted environment.

## Tests and package

Fast security suite (H2-backed):

```powershell
mvn clean test
```

A separate MySQL 8 Testcontainers integration test runs when Docker Desktop is available and is skipped when Docker is unavailable. It checks MySQL migrations, Hibernate schema validation, InnoDB and UTC timestamp persistence.

Package the backend:

```powershell
mvn clean package
```

## Authentication

### Login

`POST /api/v1/auth/login` (public)

```json
{
  "email": "admin@localhost.test",
  "password": "Choose-A-Unique-Password-2026!"
}
```

The response contains `accessToken`, `expiresIn` and a safe user profile. Send the token on protected requests:

```http
Authorization: Bearer <accessToken>
```

Access tokens expire after 30 minutes by default (`AEGISCARE_JWT_TTL_MINUTES`, maximum 120). There is currently no refresh-token flow; users sign in again when the token expires.

`GET /api/v1/auth/me` returns the current account profile.

### Provision a hospital (ADMIN only)

`POST /api/v1/admin/hospitals` with a bearer token:

```json
{
  "name": "AegisCare Demo Hospital",
  "city": "Bengaluru",
  "totalBeds": 120,
  "currentOccupancy": 76,
  "erCapacity": 20,
  "erOccupancy": 11,
  "ambulances": 3,
  "hasEmergency": true,
  "hasIcu": true
}
```

### Provision staff (ADMIN only)

`POST /api/v1/admin/users`:

```json
{
  "email": "clinician@hospital.example",
  "fullName": "Dr Example",
  "password": "A-Unique-Password-2026!",
  "role": "CLINICIAN",
  "hospitalId": "<hospital-id-from-the-hospital-response>"
}
```

Supported roles: `ADMIN`, `CLINICIAN`, `COORDINATOR`. Clinical and coordination accounts must be assigned to a hospital. There is no public registration endpoint. Administrators can list accounts with `GET /api/v1/admin/users` and disable an account with `PATCH /api/v1/admin/users/{id}/disable`.

## Hospital-level data access

- `ADMIN` can access records across the network.
- `CLINICIAN` and `COORDINATOR` accounts can access data assigned to their hospital only.
- New patient and triage records are tagged with a hospital and creator ID.
- A staff account cannot reuse a patient code belonging to another hospital; the API returns `404` rather than disclosing the other facility's record.
- Existing records are backfilled to the first configured hospital during migration where possible. Review historical data assignments before using a migrated database for real workflows.

## Database and migrations

- MySQL 8 migrations: `src/main/resources/db/migration/mysql`.
- H2 development/test migrations: `src/main/resources/db/migration/h2`.
- The storage initializer selects the MySQL-specific Flyway location for `jdbc:mysql:` connections.
- MySQL uses `DATETIME(6)` for `Instant` fields and Hibernate JDBC time zone is set to UTC.
- Flyway owns schema evolution; `spring.jpa.hibernate.ddl-auto=validate` prevents Hibernate from silently changing the schema.
- A new, empty MySQL database runs V1 then V2. MySQL baselining is disabled so the migration chain cannot be applied blindly to an unknown pre-existing schema.

This is a new MySQL database target. Existing PostgreSQL/H2 data is not automatically converted. Plan and test a separate export/transform/import if historical records must be retained.

## Production environment (Render)

The root `render.yaml` defines a private MySQL 8 service with a persistent disk and a Spring Boot web service. Configure these values through Render's environment and secret management:

| Variable | Required value |
|---|---|
| `AEGISCARE_REQUIRE_MYSQL` | `true` |
| `AEGISCARE_MYSQL_JDBC_URL` | `jdbc:mysql://aegiscare-mysql:3306/aegiscare?connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true&sslMode=REQUIRED` |
| `AEGISCARE_MYSQL_USERNAME` | Render-generated service reference to `MYSQL_USER` |
| `AEGISCARE_MYSQL_PASSWORD` | Render-generated service reference to `MYSQL_PASSWORD` |
| `AEGISCARE_JWT_SECRET` | Unique random secret, at least 32 bytes |
| `AEGISCARE_DEMO_DATA` | `false` |
| `AEGISCARE_CORS_ALLOWED_ORIGINS` | Exact production Vercel origin |
| `AEGISCARE_BOOTSTRAP_ADMIN_ENABLED` | `true` only for initial provisioning; set `false` after account creation |
| `AEGISCARE_BOOTSTRAP_ADMIN_EMAIL` | Initial admin email |
| `AEGISCARE_BOOTSTRAP_ADMIN_PASSWORD` | Unique 12–72 byte password |

The database service and API must be in the same Render workspace and region. The MySQL endpoint is private; do not publish port 3306. Wait for MySQL to become live before relying on the API deployment. Render supplies `PORT` to the API container.

Set the Vercel frontend variable `VITE_API_BASE_URL` to the Render API origin (no `/api/v1` suffix). Add that exact Vercel origin to `AEGISCARE_CORS_ALLOWED_ORIGINS`. Disable bootstrap administrator creation after first login.

## Health check

`GET /api/v1/system/health` is public and checks the live database connection. It reports `status: degraded` and `storage.connected: false` when the database query fails. In MySQL production mode, the storage mode is reported as `mysql`.
