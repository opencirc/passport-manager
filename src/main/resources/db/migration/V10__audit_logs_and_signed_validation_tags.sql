-- ADR 0006: Polymorphic audit logs with RFC 6902 JSON Patch diffs and cryptographic signatures.
--
-- Replaces the passport-only passport_logs table with a unified audit_logs stream covering
-- passports, passport datasheet instances and global datasheet definitions. Each entry stores
-- the state delta as an RFC 6902 JSON Patch array and denormalises project_id and passport_id
-- for zero-join feeds. audit_log_tags records one signed validation state per log entry.
--
-- Forward-only: rows in passport_logs are not migrated.

DROP TABLE IF EXISTS public.passport_logs;

CREATE TABLE public.audit_logs (
    id VARCHAR(100) PRIMARY KEY DEFAULT uuid_generate_v4()::VARCHAR,
    target_type VARCHAR(40) NOT NULL,
    target_id VARCHAR(100) NOT NULL,
    project_id VARCHAR(100),
    passport_id VARCHAR(100),
    action VARCHAR(20) NOT NULL,
    diff JSONB NOT NULL,
    created_by_id VARCHAR(100),
    created_time TIMESTAMP(6) WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_audit_logs_project FOREIGN KEY (project_id) REFERENCES public.projects (id) ON DELETE CASCADE,
    CONSTRAINT fk_audit_logs_passport FOREIGN KEY (passport_id) REFERENCES public.passports (id) ON DELETE CASCADE,
    CONSTRAINT fk_audit_logs_created_by FOREIGN KEY (created_by_id) REFERENCES public.api_keys (id) ON DELETE SET NULL
);

CREATE INDEX idx_audit_logs_target ON public.audit_logs (target_type, target_id, created_time ASC);
CREATE INDEX idx_audit_logs_project ON public.audit_logs (project_id, created_time DESC);
CREATE INDEX idx_audit_logs_passport ON public.audit_logs (passport_id, created_time DESC);

CREATE TABLE public.audit_log_tags (
    id VARCHAR(100) PRIMARY KEY DEFAULT uuid_generate_v4()::VARCHAR,
    log_id VARCHAR(100) NOT NULL,
    tag VARCHAR(20) NOT NULL,
    email TEXT NOT NULL,
    api_key_id VARCHAR(100) NOT NULL,
    signature TEXT NOT NULL,
    created_time TIMESTAMP(6) WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_time TIMESTAMP(6) WITH TIME ZONE,
    CONSTRAINT fk_audit_log_tags_log FOREIGN KEY (log_id) REFERENCES public.audit_logs (id) ON DELETE CASCADE,
    CONSTRAINT fk_audit_log_tags_api_key FOREIGN KEY (api_key_id) REFERENCES public.api_keys (id) ON DELETE RESTRICT,
    CONSTRAINT uq_audit_log_tags_log_id UNIQUE (log_id)
);

CREATE INDEX idx_audit_log_tags_email ON public.audit_log_tags (email);
CREATE INDEX idx_audit_log_tags_tag ON public.audit_log_tags (tag);
