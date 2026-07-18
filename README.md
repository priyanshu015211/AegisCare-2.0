# AegisCare &mdash; Rebuilt

The original project was a Python/Streamlit UI on a FastAPI backend that
only wrote to Supabase &mdash; if Supabase wasn't configured, writes were
silently dropped (`get_db_service()` returned `None` and callers just
caught the exception and moved on). This rebuild replaces both pieces:

- **`backend/`** &mdash; Java 21 / Spring Boot REST API. Same domain logic
  (rule-based triage risk scoring, hospitals, doctors, appointments,
  escalation queue) as the original Python services, ported over.
- **`frontend/`** &mdash; TypeScript + Tailwind CSS v4, built with Vite. No
  React/framework baggage, no default template look &mdash; a dense,
  clinical console UI (Space Grotesk headings, IBM Plex Mono for data,
  hand-drawn SVG charts instead of a generic charting library skin).

## Storage, specifically

`StorageDataSourceInitializer` runs before Spring wires up the datasource:

- If `SUPABASE_JDBC_URL` (+ credentials) is set **and** reachable, all
  data goes to Supabase Postgres.
- Otherwise it automatically falls back to a local file-backed H2
  database (`backend/data/aegiscare.mv.db`), so data always persists
  somewhere real. The active mode is visible live in the frontend's top
  bar and via `GET /api/v1/system/health`.

## Running it

**Backend** (needs Java 21 + Maven; downloads Spring Boot/Postgres/H2
from Maven Central on first run):

```bash
cd backend
mvn spring-boot:run
```

Runs on `http://localhost:8080`. On first boot it seeds a few hospitals,
doctors, and triage sessions so the dashboard isn't empty.

**Frontend**:

```bash
cd frontend
npm install
npm run dev
```

Runs on `http://localhost:5173` and proxies `/api/*` to the backend on
port 8080 (see `vite.config.ts`). For a production build: `npm run build`
(already verified to type-check and build cleanly).

## What maps to what

| Old Streamlit page      | New route        | Backend endpoint(s) |
|--------------------------|-------------------|----------------------|
| `dashboard.py`           | `#dashboard`      | `GET /api/v1/dashboard/summary` |
| `patient_triage.py`      | `#triage`         | `POST /api/v1/ai/analyze` |
| `emergency_center.py`    | `#emergency`      | `GET /api/v1/emergency/high-risk` |
| `coordination_dashboard.py` | `#coordination` | `GET /api/v1/coordination/summary`, `/queue`, `/doctors` |
| `analytics.py`           | `#analytics`      | `GET /api/v1/analytics/volume-trend`, `/escalation-frequency` |

The original pages were mostly hardcoded placeholder data (`st.dataframe`
with literal Python dicts). Every number in the new UI now comes from the
database instead.

## A note on scope

The original repo also had voice (STT/TTS), a broader AI reasoning/memory
pipeline, and auth middleware that weren't reflected in the Streamlit UI
at all (the pages either used hardcoded data or called only
`/api/v1/ai/analyze`). This rebuild focuses on what the UI actually
exercises: triage risk scoring, dashboard/emergency/coordination/analytics
views, and durable storage. The risk-scoring rules themselves are ported
directly from `backend/services/risk_service.py` and
`backend/services/patient_service.py` in the original.

I wasn't able to compile the Java backend inside this sandbox (Maven
Central isn't reachable from here), so double-check `mvn spring-boot:run`
on your machine &mdash; the frontend, by contrast, was fully installed,
type-checked, and built successfully here.
