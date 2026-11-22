-- MySQL 8 initial AegisCare schema.
-- MySQL deployment migration chain for a new, empty MySQL 8 database.
-- Existing PostgreSQL/H2 data is not migrated by these DDL scripts.

CREATE TABLE hospitals (
    id VARCHAR(255) NOT NULL PRIMARY KEY,
    name VARCHAR(255),
    city VARCHAR(255),
    total_beds INTEGER DEFAULT 0,
    current_occupancy INTEGER DEFAULT 0,
    er_capacity INTEGER DEFAULT 0,
    er_occupancy INTEGER DEFAULT 0,
    ambulances INTEGER DEFAULT 0,
    has_emergency BOOLEAN DEFAULT TRUE,
    has_icu BOOLEAN DEFAULT TRUE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE doctors (
    id VARCHAR(255) NOT NULL PRIMARY KEY,
    full_name VARCHAR(255),
    specialty VARCHAR(255),
    department VARCHAR(255),
    hospital_id VARCHAR(255),
    status VARCHAR(255) DEFAULT 'available'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE patients (
    id VARCHAR(255) NOT NULL PRIMARY KEY,
    patient_code VARCHAR(255) UNIQUE,
    full_name VARCHAR(255),
    age INTEGER,
    gender VARCHAR(255),
    phone VARCHAR(255),
    known_conditions VARCHAR(255),
    current_medications VARCHAR(255),
    allergies VARCHAR(255),
    created_at DATETIME(6)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE triage_sessions (
    id VARCHAR(255) NOT NULL PRIMARY KEY,
    patient_code VARCHAR(255),
    symptoms VARCHAR(2000),
    duration VARCHAR(255),
    severity VARCHAR(255),
    risk_score INTEGER,
    reasoning VARCHAR(4000),
    follow_up_question VARCHAR(1000),
    escalation_needed BOOLEAN,
    status VARCHAR(255),
    started_at DATETIME(6)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE appointments (
    id VARCHAR(255) NOT NULL PRIMARY KEY,
    patient_code VARCHAR(255),
    doctor_id VARCHAR(255),
    scheduled_at DATETIME(6),
    status VARCHAR(255),
    appointment_type VARCHAR(255),
    notes VARCHAR(255)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX idx_triage_started_at ON triage_sessions(started_at);
CREATE INDEX idx_triage_risk_score ON triage_sessions(risk_score);
CREATE INDEX idx_doctors_hospital ON doctors(hospital_id);
