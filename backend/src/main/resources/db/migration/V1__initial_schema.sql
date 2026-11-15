-- ElderCare 360 — Initial Database Schema
-- Migration V1: Core tables

-- Users
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    phone VARCHAR(20),
    role VARCHAR(30) NOT NULL CHECK (role IN ('ELDERLY','FAMILY_MEMBER','CAREGIVER','ADMIN')),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    profile_image_url VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_role ON users(role);

-- Elderly profiles (extended data for elderly users)
CREATE TABLE elderly_profiles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    date_of_birth DATE,
    blood_group VARCHAR(5),
    allergies TEXT,
    chronic_conditions TEXT,
    primary_doctor VARCHAR(200),
    primary_hospital VARCHAR(200),
    emergency_notes TEXT,
    address TEXT,
    city VARCHAR(100),
    state VARCHAR(100),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Family invitations
CREATE TABLE family_invitations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    elderly_user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    invited_by_user_id UUID NOT NULL REFERENCES users(id),
    invitee_email VARCHAR(255) NOT NULL,
    invitee_role VARCHAR(30) NOT NULL CHECK (invitee_role IN ('FAMILY_MEMBER','CAREGIVER')),
    invitation_token VARCHAR(255) NOT NULL UNIQUE,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING','ACCEPTED','REJECTED','EXPIRED','REVOKED')),
    permissions JSONB NOT NULL DEFAULT '{}',
    message TEXT,
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_invitations_token ON family_invitations(invitation_token);
CREATE INDEX idx_invitations_elderly ON family_invitations(elderly_user_id);
CREATE INDEX idx_invitations_email ON family_invitations(invitee_email);

-- Caregiver relationships
CREATE TABLE caregiver_relationships (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    elderly_user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    related_user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    relationship_type VARCHAR(30) NOT NULL CHECK (relationship_type IN ('FAMILY_MEMBER','CAREGIVER')),
    permissions JSONB NOT NULL DEFAULT '{}',
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    invitation_id UUID REFERENCES family_invitations(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(elderly_user_id, related_user_id)
);

CREATE INDEX idx_relationships_elderly ON caregiver_relationships(elderly_user_id);
CREATE INDEX idx_relationships_related ON caregiver_relationships(related_user_id);

-- Emergency contacts
CREATE TABLE emergency_contacts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    elderly_user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name VARCHAR(200) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    email VARCHAR(255),
    relationship VARCHAR(100),
    is_primary BOOLEAN NOT NULL DEFAULT FALSE,
    notify_on_sos BOOLEAN NOT NULL DEFAULT TRUE,
    notify_on_missed_dose BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_emergency_contacts_elderly ON emergency_contacts(elderly_user_id);

-- Medications
CREATE TABLE medications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    elderly_user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_by_user_id UUID NOT NULL REFERENCES users(id),
    name VARCHAR(255) NOT NULL,
    generic_name VARCHAR(255),
    dosage_text VARCHAR(200) NOT NULL,
    medicine_type VARCHAR(50) NOT NULL CHECK (medicine_type IN ('TABLET','CAPSULE','SYRUP','INJECTION','DROPS','INHALER','PATCH','CREAM','OTHER')),
    frequency VARCHAR(50) NOT NULL CHECK (frequency IN ('ONCE_DAILY','TWICE_DAILY','THREE_TIMES_DAILY','FOUR_TIMES_DAILY','EVERY_8_HOURS','EVERY_12_HOURS','WEEKLY','AS_NEEDED','CUSTOM')),
    start_date DATE NOT NULL,
    end_date DATE,
    scheduled_times JSONB NOT NULL DEFAULT '[]',
    instructions TEXT,
    prescribing_doctor VARCHAR(200),
    notes TEXT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    color VARCHAR(7) DEFAULT '#3B82F6',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_medications_elderly ON medications(elderly_user_id);
CREATE INDEX idx_medications_active ON medications(is_active);

-- Medication schedules (generated dose records)
CREATE TABLE medication_schedules (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    medication_id UUID NOT NULL REFERENCES medications(id) ON DELETE CASCADE,
    elderly_user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    scheduled_date DATE NOT NULL,
    scheduled_time TIME NOT NULL,
    scheduled_datetime TIMESTAMPTZ NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING','TAKEN','SNOOZED','SKIPPED','UNCONFIRMED')),
    taken_at TIMESTAMPTZ,
    confirmed_by_user_id UUID REFERENCES users(id),
    snooze_count INTEGER NOT NULL DEFAULT 0,
    snoozed_until TIMESTAMPTZ,
    skip_reason TEXT,
    escalation_sent BOOLEAN NOT NULL DEFAULT FALSE,
    escalation_sent_at TIMESTAMPTZ,
    escalation_acknowledged BOOLEAN NOT NULL DEFAULT FALSE,
    escalation_acknowledged_by UUID REFERENCES users(id),
    escalation_acknowledged_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(medication_id, scheduled_datetime)
);

CREATE INDEX idx_schedules_elderly ON medication_schedules(elderly_user_id);
CREATE INDEX idx_schedules_date ON medication_schedules(scheduled_date);
CREATE INDEX idx_schedules_status ON medication_schedules(status);
CREATE INDEX idx_schedules_datetime ON medication_schedules(scheduled_datetime);

-- Medical documents
CREATE TABLE medical_documents (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    elderly_user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    uploaded_by_user_id UUID NOT NULL REFERENCES users(id),
    title VARCHAR(300) NOT NULL,
    category VARCHAR(50) NOT NULL CHECK (category IN (
        'PRESCRIPTION','CBC_REPORT','BLOOD_GLUCOSE','HBA1C','KIDNEY_FUNCTION',
        'LIVER_FUNCTION','LIPID_PROFILE','XRAY','MRI','CT_SCAN','ULTRASOUND',
        'DISCHARGE_SUMMARY','CONSULTATION_NOTES','VACCINATION_RECORD','OTHER'
    )),
    document_date DATE,
    doctor_name VARCHAR(200),
    hospital_name VARCHAR(200),
    notes TEXT,
    file_name VARCHAR(300) NOT NULL,
    file_storage_key VARCHAR(500) NOT NULL UNIQUE,
    file_format VARCHAR(10) NOT NULL CHECK (file_format IN ('PDF','JPG','JPEG','PNG')),
    file_size_bytes BIGINT NOT NULL,
    sha256_checksum VARCHAR(64) NOT NULL,
    ocr_status VARCHAR(20) NOT NULL DEFAULT 'NOT_PROCESSED' CHECK (ocr_status IN ('NOT_PROCESSED','PROCESSING','COMPLETED','FAILED','NOT_APPLICABLE')),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_documents_elderly ON medical_documents(elderly_user_id);
CREATE INDEX idx_documents_category ON medical_documents(category);
CREATE INDEX idx_documents_date ON medical_documents(document_date);
CREATE INDEX idx_documents_deleted ON medical_documents(is_deleted);

-- Medical document OCR text
CREATE TABLE medical_document_text (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    document_id UUID NOT NULL UNIQUE REFERENCES medical_documents(id) ON DELETE CASCADE,
    raw_text TEXT,
    extracted_fields JSONB DEFAULT '{}',
    ocr_confidence DOUBLE PRECISION,
    page_count INTEGER,
    processed_at TIMESTAMPTZ,
    processing_duration_ms BIGINT,
    error_message TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Document permissions
CREATE TABLE document_permissions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    document_id UUID NOT NULL REFERENCES medical_documents(id) ON DELETE CASCADE,
    granted_to_user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    granted_by_user_id UUID NOT NULL REFERENCES users(id),
    can_view BOOLEAN NOT NULL DEFAULT TRUE,
    can_download BOOLEAN NOT NULL DEFAULT FALSE,
    expires_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(document_id, granted_to_user_id)
);

CREATE INDEX idx_doc_permissions_document ON document_permissions(document_id);
CREATE INDEX idx_doc_permissions_user ON document_permissions(granted_to_user_id);

-- Document audit logs
CREATE TABLE document_audit_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    document_id UUID NOT NULL REFERENCES medical_documents(id),
    user_id UUID NOT NULL REFERENCES users(id),
    action VARCHAR(50) NOT NULL CHECK (action IN ('UPLOAD','VIEW','DOWNLOAD','UPDATE_METADATA','DELETE','SHARE','REVOKE_SHARE','OCR_PROCESS')),
    ip_address VARCHAR(45),
    user_agent VARCHAR(500),
    details JSONB DEFAULT '{}',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_doc_audit_document ON document_audit_logs(document_id);
CREATE INDEX idx_doc_audit_user ON document_audit_logs(user_id);

-- Appointments
CREATE TABLE appointments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    elderly_user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_by_user_id UUID NOT NULL REFERENCES users(id),
    doctor_name VARCHAR(200) NOT NULL,
    hospital_name VARCHAR(200),
    appointment_datetime TIMESTAMPTZ NOT NULL,
    purpose VARCHAR(300) NOT NULL,
    notes TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED' CHECK (status IN ('SCHEDULED','COMPLETED','CANCELLED','RESCHEDULED')),
    reminder_sent BOOLEAN NOT NULL DEFAULT FALSE,
    reminder_sent_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_appointments_elderly ON appointments(elderly_user_id);
CREATE INDEX idx_appointments_datetime ON appointments(appointment_datetime);
CREATE INDEX idx_appointments_status ON appointments(status);

-- Care tasks
CREATE TABLE care_tasks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    elderly_user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    assigned_to_user_id UUID REFERENCES users(id),
    created_by_user_id UUID NOT NULL REFERENCES users(id),
    title VARCHAR(300) NOT NULL,
    description TEXT,
    task_type VARCHAR(50) NOT NULL CHECK (task_type IN ('MEDICINE_FOLLOWUP','APPOINTMENT_ASSIST','REPORT_COLLECTION','ROUTINE_CHECKIN','GENERAL_CARE','OTHER')),
    priority VARCHAR(10) NOT NULL DEFAULT 'MEDIUM' CHECK (priority IN ('LOW','MEDIUM','HIGH','URGENT')),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING','IN_PROGRESS','COMPLETED','CANCELLED')),
    due_date DATE,
    completion_notes TEXT,
    completed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_care_tasks_elderly ON care_tasks(elderly_user_id);
CREATE INDEX idx_care_tasks_assigned ON care_tasks(assigned_to_user_id);
CREATE INDEX idx_care_tasks_status ON care_tasks(status);

-- Notifications
CREATE TABLE notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title VARCHAR(300) NOT NULL,
    message TEXT NOT NULL,
    notification_type VARCHAR(50) NOT NULL CHECK (notification_type IN (
        'MEDICATION_REMINDER','MEDICATION_FOLLOWUP','CAREGIVER_ESCALATION',
        'APPOINTMENT_REMINDER','CARE_TASK_UPDATE','EMERGENCY_ALERT',
        'INVITATION','SYSTEM','DOSE_MISSED'
    )),
    related_entity_type VARCHAR(50),
    related_entity_id UUID,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    read_at TIMESTAMPTZ,
    email_sent BOOLEAN NOT NULL DEFAULT FALSE,
    email_sent_at TIMESTAMPTZ,
    email_error TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_notifications_user ON notifications(user_id);
CREATE INDEX idx_notifications_read ON notifications(is_read);
CREATE INDEX idx_notifications_created ON notifications(created_at DESC);
CREATE INDEX idx_notifications_type ON notifications(notification_type);

-- Emergency events
CREATE TABLE emergency_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    elderly_user_id UUID NOT NULL REFERENCES users(id),
    triggered_by_user_id UUID NOT NULL REFERENCES users(id),
    event_type VARCHAR(30) NOT NULL DEFAULT 'SOS' CHECK (event_type IN ('SOS','FALL_DETECTED','MANUAL')),
    location_text VARCHAR(500),
    notes TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE','ACKNOWLEDGED','RESOLVED')),
    contacts_notified JSONB DEFAULT '[]',
    acknowledged_at TIMESTAMPTZ,
    resolved_at TIMESTAMPTZ,
    is_demo BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_emergency_events_elderly ON emergency_events(elderly_user_id);

-- General audit logs
CREATE TABLE audit_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id),
    action VARCHAR(100) NOT NULL,
    entity_type VARCHAR(100),
    entity_id UUID,
    old_values JSONB,
    new_values JSONB,
    ip_address VARCHAR(45),
    user_agent VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_audit_logs_user ON audit_logs(user_id);
CREATE INDEX idx_audit_logs_entity ON audit_logs(entity_type, entity_id);
CREATE INDEX idx_audit_logs_created ON audit_logs(created_at DESC);
