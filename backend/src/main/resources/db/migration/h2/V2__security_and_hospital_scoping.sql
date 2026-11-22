CREATE TABLE IF NOT EXISTS user_accounts (
    id VARCHAR(36) PRIMARY KEY,
    email VARCHAR(320) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    full_name VARCHAR(120) NOT NULL,
    account_role VARCHAR(24) NOT NULL,
    hospital_id VARCHAR(255),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

ALTER TABLE patients ADD COLUMN IF NOT EXISTS hospital_id VARCHAR(255);
ALTER TABLE patients ADD COLUMN IF NOT EXISTS created_by_user_id VARCHAR(36);
ALTER TABLE triage_sessions ADD COLUMN IF NOT EXISTS hospital_id VARCHAR(255);
ALTER TABLE triage_sessions ADD COLUMN IF NOT EXISTS created_by_user_id VARCHAR(36);
ALTER TABLE appointments ADD COLUMN IF NOT EXISTS hospital_id VARCHAR(255);
ALTER TABLE appointments ADD COLUMN IF NOT EXISTS created_by_user_id VARCHAR(36);

-- Preserve the legacy demo dataset by assigning existing rows to their
-- original network's first hospital. New records are always explicitly scoped.
UPDATE patients
SET hospital_id = (SELECT id FROM hospitals ORDER BY name LIMIT 1)
WHERE hospital_id IS NULL AND EXISTS (SELECT 1 FROM hospitals);

UPDATE triage_sessions
SET hospital_id = (SELECT p.hospital_id FROM patients p
                   WHERE p.patient_code = triage_sessions.patient_code)
WHERE hospital_id IS NULL;

UPDATE appointments
SET hospital_id = (SELECT d.hospital_id FROM doctors d
                   WHERE d.id = appointments.doctor_id)
WHERE hospital_id IS NULL;

-- Older installations may contain orphaned facility identifiers. Keep those
-- legacy records visible only to administrators until they are reconciled,
-- rather than failing the entire migration or assigning them to the wrong site.
UPDATE doctors SET hospital_id = NULL
WHERE hospital_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM hospitals h WHERE h.id = doctors.hospital_id);
UPDATE patients SET hospital_id = NULL
WHERE hospital_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM hospitals h WHERE h.id = patients.hospital_id);
UPDATE triage_sessions SET hospital_id = NULL
WHERE hospital_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM hospitals h WHERE h.id = triage_sessions.hospital_id);
UPDATE appointments SET hospital_id = NULL
WHERE hospital_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM hospitals h WHERE h.id = appointments.hospital_id);
UPDATE patients SET created_by_user_id = NULL
WHERE created_by_user_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM user_accounts u WHERE u.id = patients.created_by_user_id);
UPDATE triage_sessions SET created_by_user_id = NULL
WHERE created_by_user_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM user_accounts u WHERE u.id = triage_sessions.created_by_user_id);
UPDATE appointments SET created_by_user_id = NULL
WHERE created_by_user_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM user_accounts u WHERE u.id = appointments.created_by_user_id);

CREATE INDEX IF NOT EXISTS idx_user_accounts_hospital ON user_accounts(hospital_id);
CREATE INDEX IF NOT EXISTS idx_user_accounts_role ON user_accounts(account_role);
CREATE INDEX IF NOT EXISTS idx_patients_hospital ON patients(hospital_id);
CREATE INDEX IF NOT EXISTS idx_triage_hospital_started ON triage_sessions(hospital_id, started_at);
CREATE INDEX IF NOT EXISTS idx_appointments_hospital_scheduled ON appointments(hospital_id, scheduled_at);

-- Enforce the facility boundary at the database layer as well as in the API.
ALTER TABLE doctors ADD CONSTRAINT fk_doctors_hospital
    FOREIGN KEY (hospital_id) REFERENCES hospitals(id);
ALTER TABLE patients ADD CONSTRAINT fk_patients_hospital
    FOREIGN KEY (hospital_id) REFERENCES hospitals(id);
ALTER TABLE triage_sessions ADD CONSTRAINT fk_triage_hospital
    FOREIGN KEY (hospital_id) REFERENCES hospitals(id);
ALTER TABLE appointments ADD CONSTRAINT fk_appointments_hospital
    FOREIGN KEY (hospital_id) REFERENCES hospitals(id);
ALTER TABLE user_accounts ADD CONSTRAINT fk_users_hospital
    FOREIGN KEY (hospital_id) REFERENCES hospitals(id);
ALTER TABLE patients ADD CONSTRAINT fk_patients_creator
    FOREIGN KEY (created_by_user_id) REFERENCES user_accounts(id);
ALTER TABLE triage_sessions ADD CONSTRAINT fk_triage_creator
    FOREIGN KEY (created_by_user_id) REFERENCES user_accounts(id);
ALTER TABLE appointments ADD CONSTRAINT fk_appointments_creator
    FOREIGN KEY (created_by_user_id) REFERENCES user_accounts(id);
