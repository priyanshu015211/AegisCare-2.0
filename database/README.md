# AegisCare MySQL 8 operations

## Local development

1. Copy `database/.env.example` to the repository root as `.env`.
2. Replace the app and root password placeholders with unique local-only passwords.
3. Start MySQL 8 with `docker compose up -d mysql`.
4. Wait for `docker compose ps` to report the service as healthy.
5. Set the backend environment variables documented in the root README.

The Compose service binds port 3306 to `127.0.0.1` only and persists data in the named `aegiscare_mysql_data` volume.

## Render deployment

The root `render.yaml` declares MySQL as a private Docker service. The Dockerfile exposes TCP port `3306` for Render's private-service port detection. Its only persistent data directory must be mounted at `/var/lib/mysql`. The backend connects to `aegiscare-mysql:3306` over Render's private network. Do not add a public port mapping or expose the database to the internet. The backend's Docker paths are relative to its `rootDir: backend`, so the Blueprint uses `dockerfilePath: ./Dockerfile` and `dockerContext: .` for that service.

Render provisions generated values for the app and root passwords. The backend receives only the least-privilege `MYSQL_USER` and `MYSQL_PASSWORD`; it does not receive the root password.

## Backups

Use `mysqldump` for logical, database-consistent backups. A disk snapshot is not a substitute for a database-aware backup.

From the MySQL service shell, a basic backup command is:

```sh
umask 077
mysqldump --single-transaction --routines --triggers --events --hex-blob \
  --user="$MYSQL_USER" --password="$MYSQL_PASSWORD" "$MYSQL_DATABASE" \
  | gzip > "/tmp/aegiscare-$(date -u +%Y%m%dT%H%M%SZ).sql.gz"
```

Download the dump from the service environment, encrypt it, and store it in a separate backup location. Do not leave the only backup under `/tmp` or on the same disk as the live database. Perform and document periodic restore tests before storing real patient information.

## Migration notes

- MySQL migrations live in `backend/src/main/resources/db/migration/mysql`.
- H2-only compatibility migrations remain in `backend/src/main/resources/db/migration/h2` for fast local security tests and the explicitly development-only fallback.
- The storage initializer selects the MySQL migration location for `jdbc:mysql:` connections.
- MySQL DDL is versioned by Flyway. Do not edit migrations after they have been applied in an environment; add a new versioned migration instead.
- This is a new MySQL schema. Existing PostgreSQL/H2 data is not automatically converted. If historical records must be retained, plan a separately tested export/transform/import before switching users to the MySQL database.
