CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE IF NOT EXISTS public.users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    username VARCHAR(255) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,
    is_active BOOLEAN NOT NULL,
    refresh_token VARCHAR(255),
    created_by VARCHAR(255),
    created_time TIMESTAMP(6) WITHOUT TIME ZONE
);

CREATE TABLE IF NOT EXISTS public.datasheets (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    data JSONB,
    data_category VARCHAR(20),
    data_dictionary VARCHAR(50),
    created_by VARCHAR(255),
    created_time TIMESTAMP(6) WITHOUT TIME ZONE
);


CREATE TABLE IF NOT EXISTS public.passports (
    id VARCHAR(100) PRIMARY KEY,
    name VARCHAR(255),
    status VARCHAR(50),
    parent_id VARCHAR(100),
    created_by VARCHAR(255),
    created_time TIMESTAMP(6) WITHOUT TIME ZONE
);

CREATE TABLE IF NOT EXISTS public.passport_datasheet_mappings (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    passport_id VARCHAR(100),
    datasheet_id UUID,
    CONSTRAINT fk_passport FOREIGN KEY (passport_id) REFERENCES public.passports (id) ON DELETE CASCADE,
    CONSTRAINT fk_datasheet FOREIGN KEY (datasheet_id) REFERENCES public.datasheets (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS public.passport_templates (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(255),
    template JSONB,
    created_by VARCHAR(255),
    created_time TIMESTAMP(6) WITHOUT TIME ZONE
);

CREATE TABLE IF NOT EXISTS passport_logs
(
    id SERIAL PRIMARY KEY,
    passport_id VARCHAR(100) NOT NULL,
    data JSON NOT NULL,
    created_by VARCHAR(255) NOT NULL,
    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (passport_id) REFERENCES passports(id)
);

CREATE TABLE IF NOT EXISTS passport_lifecycles
(
    id SERIAL PRIMARY KEY,
    passport_id VARCHAR(100) NOT NULL,
    event_type VARCHAR(255) NOT NULL,
    data JSON NOT NULL,
    created_by VARCHAR(255) NOT NULL,
    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (passport_id) REFERENCES passports(id)
);

CREATE TABLE IF NOT EXISTS public.jwt_configs
(
    secret_key character varying(255) COLLATE pg_catalog."default" NOT NULL,
    CONSTRAINT jwt_configs_pkey PRIMARY KEY (secret_key)
);
