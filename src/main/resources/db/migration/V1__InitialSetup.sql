-- Enable uuid-ossp extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Users table
CREATE TABLE IF NOT EXISTS public.users (
    id VARCHAR(100) PRIMARY KEY DEFAULT uuid_generate_v4(),
    first_name VARCHAR(255) NOT NULL,
    last_name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,
    is_active BOOLEAN NOT NULL,
    refresh_token VARCHAR(255),
    created_time TIMESTAMP(6) WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Datasheets table
CREATE TABLE IF NOT EXISTS public.datasheets (
    id VARCHAR(100) PRIMARY KEY DEFAULT uuid_generate_v4(),
    platform VARCHAR(30),
    dictionary VARCHAR(30),
    code VARCHAR(100),
    name VARCHAR(100),
    description VARCHAR(200),
    platform_id VARCHAR(100),
    data_category VARCHAR(20),
    data JSONB,
    created_by_id VARCHAR(255),
    created_by jsonb NOT NULL,
    created_time TIMESTAMP(6) WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_datasheets_platform ON public.datasheets(platform);
CREATE INDEX IF NOT EXISTS idx_datasheets_dictionary ON public.datasheets(dictionary);
CREATE INDEX IF NOT EXISTS idx_datasheets_code ON public.datasheets(code);
CREATE INDEX IF NOT EXISTS idx_datasheets_name ON public.datasheets(name);
CREATE INDEX IF NOT EXISTS idx_datasheets_platform_id ON public.datasheets(platform_id);
CREATE INDEX IF NOT EXISTS idx_datasheets_data_category ON public.datasheets(data_category);
CREATE INDEX IF NOT EXISTS idx_datasheets_created_by_id ON public.datasheets(created_by_id);


-- Datasheet Property table
CREATE TABLE IF NOT EXISTS public.datasheet_property (
    id VARCHAR(100) PRIMARY KEY DEFAULT uuid_generate_v4(),
    code VARCHAR(50),
    platform_id VARCHAR(100),
    group_tag VARCHAR(50),
    property_type VARCHAR(200),
    definition JSONB,
    datasheet_id VARCHAR(100) NOT NULL,
    CONSTRAINT fk_datasheet FOREIGN KEY (datasheet_id) REFERENCES public.datasheets (id) ON DELETE CASCADE
    
);

CREATE INDEX IF NOT EXISTS idx_datasheet_property_code 
    ON public.datasheet_property(code);

CREATE INDEX IF NOT EXISTS idx_datasheet_property_platform_id 
    ON public.datasheet_property(platform_id);

CREATE INDEX IF NOT EXISTS idx_datasheet_property_group_tag
    ON public.datasheet_property(group_tag);

CREATE INDEX IF NOT EXISTS idx_datasheet_property_datasheet_id 
    ON public.datasheet_property(datasheet_id);

-- Passports table
CREATE TABLE IF NOT EXISTS public.passports (
    id VARCHAR(100) PRIMARY KEY,
    name VARCHAR(255),
    status VARCHAR(50),
    parent_id VARCHAR(100),
    created_by_id VARCHAR(255),
    created_by jsonb NOT NULL,
    created_time TIMESTAMP(6) WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_passports_created_by_id ON public.passports(created_by_id);

-- Passport-Datasheet mappings
CREATE TABLE IF NOT EXISTS public.passport_datasheet_mappings (
    id VARCHAR(100) PRIMARY KEY DEFAULT uuid_generate_v4(),
    passport_id VARCHAR(100) NOT NULL,
    datasheet_id VARCHAR(100) NOT NULL,
    CONSTRAINT fk_passport FOREIGN KEY (passport_id) REFERENCES public.passports (id) ON DELETE CASCADE,
    CONSTRAINT fk_datasheet FOREIGN KEY (datasheet_id) REFERENCES public.datasheets (id) ON DELETE CASCADE,
    CONSTRAINT uq_passport_datasheet UNIQUE (passport_id, datasheet_id)
);

CREATE INDEX IF NOT EXISTS idx_pdm_passport_id ON public.passport_datasheet_mappings(passport_id);
CREATE INDEX IF NOT EXISTS idx_pdm_datasheet_id ON public.passport_datasheet_mappings(datasheet_id);

-- Passport templates
CREATE TABLE IF NOT EXISTS public.passport_templates (
    id VARCHAR(100) PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(255),
    template JSONB,
    created_by_id VARCHAR(255),
    created_by jsonb NOT NULL,
    created_time TIMESTAMP(6) WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_templates_created_by_id ON public.passport_templates(created_by_id);

-- Passport logs
CREATE TABLE IF NOT EXISTS public.passport_logs (
    id VARCHAR(100) PRIMARY KEY DEFAULT uuid_generate_v4(),
    passport_id VARCHAR(100) NOT NULL,
    data JSON NOT NULL,
    created_by_id VARCHAR(255),
    created_by jsonb NOT NULL,
    created_time TIMESTAMP(6) WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (passport_id) REFERENCES public.passports(id) ON DELETE CASCADE
);


CREATE INDEX IF NOT EXISTS idx_passport_logs_passport_id ON public.passport_logs(passport_id);

-- Passport lifecycles
CREATE TABLE IF NOT EXISTS public.passport_lifecycles (
    id VARCHAR(100) PRIMARY KEY DEFAULT uuid_generate_v4(),
    passport_id VARCHAR(100) NOT NULL,
    event_type VARCHAR(255) NOT NULL,
    data JSON NOT NULL,
    created_by_id VARCHAR(255),
    created_by jsonb NOT NULL,
    created_time TIMESTAMP(6) WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
     FOREIGN KEY (passport_id) REFERENCES public.passports(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_passport_lifecycles_passport_id ON public.passport_lifecycles(passport_id);

-- JWT configs
CREATE TABLE IF NOT EXISTS public.jwt_configs (
    secret_key VARCHAR(255) NOT NULL,
    CONSTRAINT jwt_configs_pkey PRIMARY KEY (secret_key)
);

-- Api keys
CREATE TABLE IF NOT EXISTS public.api_keys (
    id VARCHAR(100) PRIMARY KEY DEFAULT uuid_generate_v4(),
    secret TEXT NOT NULL,
    user_id VARCHAR(100) NOT NULL,
    name VARCHAR(100) NOT NULL,
    created_time TIMESTAMP(6) WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    expiration_time TIMESTAMP(6) WITH TIME ZONE,
    CONSTRAINT fk_api_keys_user_id FOREIGN KEY (user_id) REFERENCES public.users (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_api_keys_user_id ON public.api_keys(user_id);
