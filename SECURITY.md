# AegisCare security foundation

This repository contains an initial application-security baseline for the AegisCare prototype. It is intended for local development, technical review and controlled deployment testing. It is **not** a security certification or approval to process real patient information.

## Authentication

- Staff authenticate through `POST /api/v1/auth/login`.
- The backend issues a short-lived signed JWT (30 minutes by default; configurable up to 120 minutes).
- Passwords are stored with BCrypt (work factor 12); plaintext passwords are never persisted.
- There is no public self-registration route. An administrator provisions staff accounts.
- The initial administrator can be created from environment variables with `AEGISCARE_BOOTSTRAP_ADMIN_ENABLED=true`. Disable this setting after first-time provisioning.
- The backend resolves the account from the database on each authenticated request. Disabled or deleted accounts are rejected, and role changes take effect without waiting for a token to expire.

## Roles and access boundaries

| Role | Intended access |
|---|---|
| `ADMIN` | Network-wide overview and administrator provisioning endpoints |
| `CLINICIAN` | Assigned-hospital workspace and clinical triage/voice actions |
| `COORDINATOR` | Assigned-hospital coordination, emergency visibility and analytics; no clinical triage submission or admin provisioning |

Non-administrator reads are filtered to the account's assigned hospital. New patient and triage records are stamped with a hospital and creator ID. The triage API returns `404` when a staff member attempts to reuse a patient code belonging to another hospital so that it does not disclose the other facility's record.

## Database lifecycle

- Flyway owns schema evolution. MySQL 8 migrations are under `backend/src/main/resources/db/migration/mysql`; H2-only development/test migrations remain under `backend/src/main/resources/db/migration/h2`.
- Hibernate is configured for `validate`, not automatic schema mutation.
- MySQL 8 is the production database. Its Flyway chain is designed for a new, empty MySQL schema; MySQL baselining is disabled to avoid applying migrations to an unknown pre-existing schema.
- Existing PostgreSQL or H2 data is not automatically converted. If historical data must be retained, perform a separately designed, tested export/transform/import and reconciliation before switching users.
- Production must set `AEGISCARE_REQUIRE_MYSQL=true`; the app waits briefly for MySQL to become reachable and then refuses to start rather than falling back to local storage. Local development can use MySQL 8 through Docker Compose or the development-only file-backed H2 fallback.
- Demo data is opt-in and must remain disabled in production (`AEGISCARE_DEMO_DATA=false`).

## Operational secrets

Set secrets through the hosting provider's secret/environment manager. Do not commit `.env` files, JWT secrets, database credentials, bootstrap passwords or API keys. Use a unique high-entropy `AEGISCARE_JWT_SECRET` of at least 32 UTF-8 bytes. Use a non-root MySQL application account for the backend; never pass the MySQL root password to the API service. Set `AEGISCARE_CORS_ALLOWED_ORIGINS` to the exact deployed frontend origin; do not use `*`.

## Current limitations — do not use real patient data

This is a prototype foundation. Before handling real patient information, the project still needs, at minimum:

- independent application and infrastructure security review;
- privacy/legal assessment for applicable health-data rules and vendor processing;
- secure cookie or hardened browser-token strategy, XSS controls and a Content Security Policy;
- authentication throttling/lockout controls, password recovery/rotation and session revocation policy;
- explicit audit-event capture, retention and tamper-evidence for sensitive reads and writes;
- encrypted database-aware backups (for example, `mysqldump --single-transaction`) and tested restore procedures, monitoring and incident response;
- end-to-end deployment testing and accessibility review;
- clinical governance and validation of triage protocols. The current `RiskScoringService` is a demonstration rule set and must not guide clinical decisions.

The public health endpoint reveals only service/storage connectivity. It does not reveal credentials or patient information.
