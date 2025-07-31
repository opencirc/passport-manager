
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'user_role') THEN
        CREATE TYPE user_role AS ENUM ('ADMIN', 'USER');
    END IF;
END $$;

CREATE TABLE IF NOT EXISTS public.users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    username VARCHAR(255) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role user_role NOT NULL,
    is_active BOOLEAN NOT NULL,
    refresh_token VARCHAR(255),
    created_by VARCHAR(255),
    created_time TIMESTAMP(6) WITHOUT TIME ZONE
);

INSERT INTO users (
    id,
    username,
    email,
    password,
    role,
    is_active,
    refresh_token,
    created_by,
    created_time
)
VALUES (
    uuid_generate_v4(),
    'admin',
    'admin@example.com',
    '$2a$12$BqyteIToCJLPd40zkR8n8ez9Snps/xDJtlYR0REyf2nHOF2RDWXeW',
    'ADMIN',
    true,
    NULL,
    'test',
    NOW()
);
INSERT INTO users (
    id,
    username,
    email,
    password,
    role,
    is_active,
    refresh_token,
    created_by,
    created_time
)
VALUES (
    uuid_generate_v4(),
    'user',
    'user@example.com',
    '$2a$12$5L/MrVN02bFWtTGM7wIihOhZGtRj6oqgfuAOx8JQX9Frajvw2rjyG',
    'USER',
    true,
    NULL,
    'test',
    NOW()
);


CREATE TABLE IF NOT EXISTS public.datasheets (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    data JSONB,
    data_category VARCHAR(20),
    data_dictionary VARCHAR(50),
    created_by VARCHAR(255),
    created_time TIMESTAMP(6) WITHOUT TIME ZONE
);

INSERT INTO public.datasheets (
    id,
    data,
    data_category,
    data_dictionary,
    created_by,
    created_time
) VALUES (
    '123e4567-e89b-12d3-a456-426614174000',
    '{
        "uri": "https://identifier.buildingsmart.org/uri/molio/cciconstruction/1.0/class/B-B__",
        "name": "Mechanical Room",
        "status": "Active",
        "classType": "Class",
        "definition": "space designed for mechanical systems and components",
        "dataCategory": "Generic",
        "templateName": "mechanicalTemplate",
        "referenceCode": "B-B__",
        "classProperties": [
            {
                "uri": "https://identifier.buildingsmart.org/uri/molio/cciconstruction/1.0/class/B-B__/prop/Pset_SpaceMechanical/uri/buildingsmart/ifc/4.3/prop/VentilationRequired",
                "name": "Ventilation Required",
                "dataType": "Boolean",
                "actualValue": "true"
            }
        ],
        "parentClassReference": {
            "uri": "https://identifier.buildingsmart.org/uri/molio/cciconstruction/1.0/class/mechanical",
            "code": "mechanical",
            "name": "Mechanical Spaces"
        },
        "relatedIfcEntityNames": ["IfcSpace"]
    }'::jsonb,
    'GENERIC',
    'BSDD',
    'admin@example.com',
    '2025-07-30 10:15:00.000000'
);



DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'passport_status') THEN
        CREATE TYPE passport_status AS ENUM ('ACTIVE', 'INACTIVE');
    END IF;
END $$;

CREATE TABLE IF NOT EXISTS public.passports (
    id VARCHAR(100) PRIMARY KEY,
    name VARCHAR(255),
    status passport_status,
    parent_id VARCHAR(100),
    created_by VARCHAR(255),
    created_time TIMESTAMP(6) WITHOUT TIME ZONE
);

INSERT INTO public.passports (
    id,
    name,
    status,
    parent_id,
    created_by,
    created_time
) VALUES (
    'cmdr1hy54000007ld3ah5fz39',
    'Test Passport',
    'ACTIVE',
    null,
    'admin@example.com',
    '2025-07-30 10:20:00.000000'
);


CREATE TABLE IF NOT EXISTS public.passport_datasheet_mappings (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    passport_id VARCHAR(100),
    datasheet_id UUID,

    CONSTRAINT fk_passport
        FOREIGN KEY (passport_id)
        REFERENCES public.passports (id)
        ON DELETE CASCADE,

    CONSTRAINT fk_datasheet
        FOREIGN KEY (datasheet_id)
        REFERENCES public.datasheets (id)
        ON DELETE CASCADE
);


INSERT INTO public.passport_datasheet_mappings (
    id,
    passport_id,
    datasheet_id
) VALUES (
    '987e6543-21cb-43ba-88ef-665b1b99fa21',
    'cmdr1hy54000007ld3ah5fz39',
    '123e4567-e89b-12d3-a456-426614174000'
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


