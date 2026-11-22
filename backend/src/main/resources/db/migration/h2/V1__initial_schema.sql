-- Initial AegisCare schema. Tables are intentionally idempotent so an
-- installation with the original Hibernate-created tables can be baselined.
CREATE TABLE IF NOT EXISTS hospitals (
    id VARCHAR(255) PRIMARY KEY,
    name VARCHAR(255),
    city VARCHAR(255),
    total_beds INTEGER DEFAULT 0,
    current_occupancy INTEGER DEFAULT 0,
    er_capacity INTEGER DEFAULT 0,
    er_occupancy INTEGER DEFAULT 0,
    ambulances INTEGER DEFAULT 0,
    has_emergency BOOLEAN DEFAULT TRUE,
    has_icu BOOLEAN DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS doctors (
    id VARCHAR(255) PRIMARY KEY,
    full_name VARCHAR(255),
    specialty VARCHAR(255),
    department VARCHAR(255),
    hospital_id VARCHAR(255),
    status VARCHAR(255) DEFAULT 'available'
);

CREATE TABLE IF NOT EXISTS patients (
    id VARCHAR(255) PRIMARY KEY,
    patient_code VARCHAR(255) UNIQUE,
    full_name VARCHAR(255),
    age INTEGER,
    gender VARCHAR(255),
    phone VARCHAR(255),
    known_conditions VARCHAR(255),
    current_medications VARCHAR(255),
    allergies VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE
);

CREATE TABLE IF NOT EXISTS triage_sessions (
    id VARCHAR(255) PRIMARY KEY,
    patient_code VARCHAR(255),
    symptoms VARCHAR(2000),
    duration VARCHAR(255),
    severity VARCHAR(255),
    risk_score INTEGER,
    reasoning VARCHAR(4000),
    follow_up_question VARCHAR(1000),
    escalation_needed BOOLEAN,
    status VARCHAR(255),
    started_at TIMESTAMP WITH TIME ZONE
);

CREATE TABLE IF NOT EXISTS appointments (
    id VARCHAR(255) PRIMARY KEY,
    patient_code VARCHAR(255),
    doctor_id VARCHAR(255),
    scheduled_at TIMESTAMP WITH TIME ZONE,
    status VARCHAR(255),
    appointment_type VARCHAR(255),
    notes VARCHAR(255)
);

CREATE INDEX IF NOT EXISTS idx_triage_started_at ON triage_sessions(started_at);
CREATE INDEX IF NOT EXISTS idx_triage_risk_score ON triage_sessions(risk_score);
CREATE INDEX IF NOT EXISTS idx_doctors_hospital ON doctors(hospital_id);
