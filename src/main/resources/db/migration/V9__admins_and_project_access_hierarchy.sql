-- ADR 0001, 0002, 0003, 0005, 0008, 0009: administrative boundary, project scoping, atomic root
-- provisioning, structured API keys with soft revocation, soft archiving and owners.
--
-- Forward-only. Passports and API keys cannot be carried over: every passport now requires a
-- project and every API key an access group, neither of which existed before. The operational
-- rows are deleted; no production data exists at this point.

-- 1) Wipe operational rows that cannot be re-homed (passport_datasheets and passport_logs cascade).
DELETE FROM public.passports;
DROP TABLE IF EXISTS public.api_keys;

-- 2) Human accounts are administrators only (ADR 0001).
ALTER TABLE public.users RENAME TO admins;
ALTER TABLE public.admins DROP COLUMN role;

-- 3) Owner organizations (ADR 0009).
CREATE TABLE public.owners (
    id VARCHAR(100) PRIMARY KEY DEFAULT uuid_generate_v4()::VARCHAR,
    name TEXT NOT NULL,
    created_time TIMESTAMP(6) WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 4) Projects (ADR 0002, ADR 0008).
CREATE TABLE public.projects (
    id VARCHAR(100) PRIMARY KEY DEFAULT uuid_generate_v4()::VARCHAR,
    owner_id VARCHAR(100),
    name TEXT NOT NULL,
    description TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'active',
    created_by_id TEXT,
    created_by JSONB NOT NULL,
    created_time TIMESTAMP(6) WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    archived_time TIMESTAMP(6) WITH TIME ZONE,
    CONSTRAINT fk_projects_owner FOREIGN KEY (owner_id) REFERENCES public.owners (id) ON DELETE SET NULL
);

CREATE INDEX idx_projects_owner_id ON public.projects(owner_id);
CREATE INDEX idx_projects_status ON public.projects(status);

-- 5) Access groups: CRUD permission profiles within a project.
CREATE TABLE public.access_groups (
    id VARCHAR(100) PRIMARY KEY DEFAULT uuid_generate_v4()::VARCHAR,
    project_id VARCHAR(100) NOT NULL,
    name TEXT NOT NULL,
    description TEXT,
    permissions JSONB NOT NULL,
    created_by_id TEXT,
    created_by JSONB NOT NULL,
    created_time TIMESTAMP(6) WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_access_groups_project FOREIGN KEY (project_id) REFERENCES public.projects (id) ON DELETE CASCADE
);

CREATE INDEX idx_access_groups_project_id ON public.access_groups(project_id);

-- 6) Machine credentials: public key id, hashed secret, soft revocation (ADR 0005).
CREATE TABLE public.api_keys (
    id VARCHAR(100) PRIMARY KEY,
    access_group_id VARCHAR(100) NOT NULL,
    secret_hash TEXT NOT NULL,
    name VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'active',
    created_time TIMESTAMP(6) WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    expiration_time TIMESTAMP(6) WITH TIME ZONE,
    CONSTRAINT fk_api_keys_access_group FOREIGN KEY (access_group_id) REFERENCES public.access_groups (id) ON DELETE CASCADE
);

CREATE INDEX idx_api_keys_access_group_id ON public.api_keys(access_group_id);

-- 7) Passports: direct project scoping, machine attribution, soft archiving (ADR 0002, ADR 0008).
ALTER TABLE public.passports DROP COLUMN created_by;
ALTER TABLE public.passports ALTER COLUMN created_by_id TYPE VARCHAR(100);
ALTER TABLE public.passports
    ADD COLUMN project_id VARCHAR(100) NOT NULL,
    ADD COLUMN archived_time TIMESTAMP(6) WITH TIME ZONE,
    ADD CONSTRAINT fk_passports_project FOREIGN KEY (project_id) REFERENCES public.projects (id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_passports_parent FOREIGN KEY (parent_id) REFERENCES public.passports (id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_passports_created_by FOREIGN KEY (created_by_id) REFERENCES public.api_keys (id) ON DELETE SET NULL;

CREATE UNIQUE INDEX uq_project_root_passport ON public.passports (project_id) WHERE parent_id IS NULL;
CREATE INDEX idx_passports_project_id ON public.passports(project_id);
CREATE INDEX idx_passports_active ON public.passports(project_id, status);
CREATE INDEX idx_passports_parent_id ON public.passports(parent_id);

-- 8) Datasheet instances are authored by machine credentials; the admin snapshot is dropped.
ALTER TABLE public.passport_datasheets DROP COLUMN created_by;
ALTER TABLE public.passport_datasheets ALTER COLUMN created_by_id TYPE VARCHAR(100);
ALTER TABLE public.passport_datasheets
    ADD CONSTRAINT fk_passport_datasheets_created_by FOREIGN KEY (created_by_id) REFERENCES public.api_keys (id) ON DELETE SET NULL;
