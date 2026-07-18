# AegisCare Backend (Java / Spring Boot)

Rewritten from the original Python/FastAPI service. Same domain (patient
triage, risk scoring, emergency coordination) but a real persistence layer
and a plain REST API for the new TypeScript frontend.

## Run

```bash
cd backend
mvn spring-boot:run
```

The API listens on `http://localhost:8080`.

## Storage

The old Python service (`backend/db/database_service.py` in the original
project) only wrote to Supabase — if Supabase wasn't configured or was
unreachable, `get_db_service()` returned `None` and every write silently
did nothing. No data was ever actually kept in that case.

This backend fixes that with a resolver (`StorageDataSourceInitializer`)
that runs before Spring wires up the datasource:

1. If `SUPABASE_JDBC_URL` (+ `SUPABASE_DB_USER` / `SUPABASE_DB_PASSWORD`)
   is set **and** a connection can actually be opened, it persists to
   Supabase Postgres.
2. Otherwise — not configured, or configured but unreachable — it
   automatically falls back to a local file-backed H2 database at
   `./data/aegiscare.mv.db`. Nothing is lost; there's no "silent no-op"
   path anymore.

Check which mode is active any time via:

```bash
curl http://localhost:8080/api/v1/system/health
```

### Pointing at Supabase

```bash
export SUPABASE_JDBC_URL="jdbc:postgresql://<project-ref>.supabase.co:5432/postgres"
export SUPABASE_DB_USER="postgres"
export SUPABASE_DB_PASSWORD="your-db-password"
mvn spring-boot:run
```

Get the JDBC connection string from Supabase → Project Settings →
Database → Connection string → JDBC.

## Data seeded on first boot

On an empty database, `DataSeeder` inserts a few hospitals, doctors,
triage sessions, and appointments so the dashboard isn't blank on first
run. It only runs when tables are empty, so it won't duplicate data or
overwrite anything on later restarts.
