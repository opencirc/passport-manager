-- Enable uuid-ossp extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Users table
CREATE TABLE IF NOT EXISTS public.users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    first_name VARCHAR(255) NOT NULL,
    last_name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,
    is_active BOOLEAN NOT NULL,
    refresh_token VARCHAR(255),
    created_by VARCHAR(255),
    created_time TIMESTAMP(6) WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Datasheets table
CREATE TABLE IF NOT EXISTS public.datasheets (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    data JSONB,
    data_category VARCHAR(20),
    data_dictionary VARCHAR(50),
    created_by VARCHAR(255),
    created_time TIMESTAMP(6) WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Passports table
CREATE TABLE IF NOT EXISTS public.passports (
    id VARCHAR(100) PRIMARY KEY,
    name VARCHAR(255),
    status VARCHAR(50),
    parent_id VARCHAR(100),
    created_by VARCHAR(255),
    created_time TIMESTAMP(6) WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Passport-Datasheet mappings
CREATE TABLE IF NOT EXISTS public.passport_datasheet_mappings (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    passport_id VARCHAR(100) NOT NULL,
    datasheet_id UUID NOT NULL,
    CONSTRAINT fk_passport FOREIGN KEY (passport_id) REFERENCES public.passports (id) ON DELETE CASCADE,
    CONSTRAINT fk_datasheet FOREIGN KEY (datasheet_id) REFERENCES public.datasheets (id) ON DELETE CASCADE,
    CONSTRAINT uq_passport_datasheet UNIQUE (passport_id, datasheet_id)
);

CREATE INDEX IF NOT EXISTS idx_pdm_passport_id ON public.passport_datasheet_mappings(passport_id);
CREATE INDEX IF NOT EXISTS idx_pdm_datasheet_id ON public.passport_datasheet_mappings(datasheet_id);

-- Passport templates
CREATE TABLE IF NOT EXISTS public.passport_templates (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(255),
    template JSONB,
    created_by VARCHAR(255),
    created_time TIMESTAMP(6) WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Passport logs
CREATE TABLE IF NOT EXISTS public.passport_logs (
    id BIGSERIAL PRIMARY KEY,
    passport_id VARCHAR(100) NOT NULL,
    data JSON NOT NULL,
    created_by VARCHAR(255) NOT NULL,
    created_time TIMESTAMP(6) WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (passport_id) REFERENCES public.passports(id) ON DELETE CASCADE
);


CREATE INDEX IF NOT EXISTS idx_passport_logs_passport_id ON public.passport_logs(passport_id);

-- Passport lifecycles
CREATE TABLE IF NOT EXISTS public.passport_lifecycles (
    id BIGSERIAL PRIMARY KEY,
    passport_id VARCHAR(100) NOT NULL,
    event_type VARCHAR(255) NOT NULL,
    data JSON NOT NULL,
    created_by VARCHAR(255) NOT NULL,
    created_time TIMESTAMP(6) WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (passport_id) REFERENCES public.passports(id)
);

-- JWT configs
CREATE TABLE IF NOT EXISTS public.jwt_configs (
    secret_key VARCHAR(255) NOT NULL,
    CONSTRAINT jwt_configs_pkey PRIMARY KEY (secret_key)
);

-- Api keys
CREATE TABLE IF NOT EXISTS  api_keys (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    secret TEXT NOT NULL,
    user_id UUID NOT NULL,
    name VARCHAR(100),
    created_time TIMESTAMP(6) WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    expiration_time TIMESTAMP(6) WITH TIME ZONE,
    CONSTRAINT fk_api_keys_user_id FOREIGN KEY (user_id) REFERENCES public.users (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_api_keys_user_id ON api_keys(user_id);
