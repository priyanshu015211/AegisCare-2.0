# AegisCare backend

Java 21 / Spring Boot REST API for hospital operations, triage recording and care coordination.

## Phase 1 foundation

- Stateless JWT bearer authentication (`POST /api/v1/auth/login`).
- Roles: `ADMIN`, `CLINICIAN`, `COORDINATOR`.
- Administrator-only account and hospital provisioning endpoints.
- Hospital-level access scoping for patients, triage sessions, emergency data, dashboards, analytics, doctors and appointment summaries.
- BCrypt password hashing (work factor 12); no public self-registration.
- Account enabled/role state is checked against the database on each authenticated request, so disabled accounts lose access immediately.
- Flyway owns schema migrations; Hibernate runs in `validate` mode.
- Development H2 fallback is available only when `AEGISCARE_REQUIRE_POSTGRES=false`. Set it to `true` in production to fail closed if PostgreSQL is missing or unreachable.
- Synthetic demo records are disabled by default. Enable them only in a local development environment.

This is an application security foundation, not a clinical certification. Do not enter real patient data until the deployment, privacy controls, backup/restore, audit requirements and clinical workflows have undergone appropriate review.

## Run locally (PowerShell)

From the `backend` directory:

```powershell
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

The API listens on `http://localhost:8080`. The bootstrap admin is created only when the configured email does not already exist. It does not reset an existing user's password. The JWT secret and bootstrap password should remain in the terminal environment or secret manager; do not commit them.

To run automated backend tests:

```powershell
mvn test
```

To package the backend:

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

Flyway scripts live under `src/main/resources/db/migration`.

- `V1__initial_schema.sql` creates the original domain tables for a new database.
- `V2__security_and_hospital_scoping.sql` creates user accounts and adds hospital/creator scope columns. Existing Hibernate-created databases are baselined at version 1, then receive V2.
- `spring.jpa.hibernate.ddl-auto=validate` prevents Hibernate from silently modifying a deployed schema.

For local development without Supabase, the application uses file-backed H2 under `backend/data/`. This is not suitable for hosted production.

## Production environment (Render)

Configure these values in Render's service environment. Do not put secrets in GitHub or `render.yaml`:

| Variable | Required value |
|---|---|
| `AEGISCARE_JWT_SECRET` | Unique random secret, at least 32 bytes |
| `AEGISCARE_REQUIRE_POSTGRES` | `true` |
| `AEGISCARE_DEMO_DATA` | `false` |
| `SUPABASE_JDBC_URL` | Supabase PostgreSQL JDBC URL with TLS, e.g. `...?sslmode=require` |
| `SUPABASE_DB_USER` | Username from Supabase connection details |
| `SUPABASE_DB_PASSWORD` | Supabase database password |
| `AEGISCARE_CORS_ALLOWED_ORIGINS` | Exact production Vercel origin, e.g. `https://your-project.vercel.app` |
| `AEGISCARE_BOOTSTRAP_ADMIN_ENABLED` | `true` for first account creation only; set `false` after it exists |
| `AEGISCARE_BOOTSTRAP_ADMIN_EMAIL` | Initial admin email |
| `AEGISCARE_BOOTSTRAP_ADMIN_PASSWORD` | Unique 12–72 byte password for the initial admin |

Render supplies `PORT`; the app binds to it automatically. With `AEGISCARE_REQUIRE_POSTGRES=true`, a missing or unreachable PostgreSQL database stops startup instead of silently switching to local H2.

Set the Vercel frontend environment variable `VITE_API_BASE_URL` to the Render API origin (no `/api/v1` suffix). Add that exact Vercel origin to `AEGISCARE_CORS_ALLOWED_ORIGINS` on Render. Local Vite development continues to use the `/api` proxy to `localhost:8080` when the variable is empty.

## Health check

`GET /api/v1/system/health` is public and checks the live database connection. It reports `status: degraded` and `storage.connected: false` when the database query fails.
