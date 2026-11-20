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

- Flyway owns schema evolution under `backend/src/main/resources/db/migration`.
- Hibernate is configured for `validate`, not automatic schema mutation.
- New databases run the initial schema and subsequent migrations.
- Existing Hibernate-created databases are baselined at version 1 and receive the security/hospital-scoping migration.
- Local development can use file-backed H2. Production must set `AEGISCARE_REQUIRE_POSTGRES=true`; the app then refuses to start if PostgreSQL is missing or unreachable.
- Demo data is opt-in and must remain disabled in production (`AEGISCARE_DEMO_DATA=false`).

## Operational secrets

Set secrets through the hosting provider's secret/environment manager. Do not commit `.env` files, JWT secrets, database credentials, bootstrap passwords or API keys. Use a unique high-entropy `AEGISCARE_JWT_SECRET` of at least 32 UTF-8 bytes. Set `AEGISCARE_CORS_ALLOWED_ORIGINS` to the exact deployed frontend origin; do not use `*`.

## Current limitations — do not use real patient data

This is a prototype foundation. Before handling real patient information, the project still needs, at minimum:

- independent application and infrastructure security review;
- privacy/legal assessment for applicable health-data rules and vendor processing;
- secure cookie or hardened browser-token strategy, XSS controls and a Content Security Policy;
- authentication throttling/lockout controls, password recovery/rotation and session revocation policy;
- explicit audit-event capture, retention and tamper-evidence for sensitive reads and writes;
- encrypted backups and tested restore procedures, monitoring and incident response;
- end-to-end deployment testing and accessibility review;
- clinical governance and validation of triage protocols. The current `RiskScoringService` is a demonstration rule set and must not guide clinical decisions.

The public health endpoint reveals only service/storage connectivity. It does not reveal credentials or patient information.
