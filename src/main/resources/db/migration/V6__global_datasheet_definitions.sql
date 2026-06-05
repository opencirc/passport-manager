-- Globalise datasheet definitions.
--
-- Previously every passport owned a fully-materialised copy of its datasheet template:
-- one `datasheets` row + a fresh set of `datasheet_properties` rows per passport, duplicating
-- the dictionary-sourced definition for every passport built from the same URI.
--
-- This migration splits the immutable, URI-keyed *definition* (shared globally) from the
-- per-passport *instance* (which carries only the values). A passport now references a shared
-- definition by foreign key instead of embedding a copy.
--
-- Data is disposable at this stage (staging only), so this is a forward-only drop & recreate.

BEGIN;

-- 1) Drop the old per-passport datasheet structures (FK order: mappings -> properties -> datasheets).
DROP TABLE IF EXISTS public.passport_datasheet_mappings;
DROP TABLE IF EXISTS public.datasheet_properties;
DROP TABLE IF EXISTS public.datasheets;

-- 2) Global, URI-keyed datasheet definitions (one row per dictionary class URI).
CREATE TABLE IF NOT EXISTS public.datasheet_definitions (
    id VARCHAR(100) PRIMARY KEY DEFAULT uuid_generate_v4()::VARCHAR,
    platform VARCHAR(30),
    dictionary VARCHAR(30),
    code TEXT,
    name TEXT,
    description TEXT,
    platform_id TEXT NOT NULL,
    related_platform_ids JSONB,
    synced_time TIMESTAMP(6) WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_datasheet_definitions_platform_id UNIQUE (platform_id)
);

CREATE INDEX IF NOT EXISTS idx_datasheet_definitions_code ON public.datasheet_definitions(code);
CREATE INDEX IF NOT EXISTS idx_datasheet_definitions_dictionary ON public.datasheet_definitions(dictionary);

-- 3) Property definitions belonging to a datasheet definition (shared globally).
CREATE TABLE IF NOT EXISTS public.datasheet_definition_properties (
    id VARCHAR(100) PRIMARY KEY DEFAULT uuid_generate_v4()::VARCHAR,
    definition_id VARCHAR(100) NOT NULL,
    code TEXT,
    platform_id TEXT,
    group_tag TEXT,
    property_type VARCHAR(200),
    definition JSONB,
    CONSTRAINT fk_definition_property_definition
        FOREIGN KEY (definition_id) REFERENCES public.datasheet_definitions (id) ON DELETE CASCADE,
    CONSTRAINT uq_definition_property_code UNIQUE (definition_id, code)
);

CREATE INDEX IF NOT EXISTS idx_definition_property_definition_id
    ON public.datasheet_definition_properties(definition_id);
CREATE INDEX IF NOT EXISTS idx_definition_property_code
    ON public.datasheet_definition_properties(code);

-- 4) Per-passport datasheet instance: links a passport to a shared definition and holds the values.
--    Absorbs the old passport_datasheet_mappings table.
CREATE TABLE IF NOT EXISTS public.passport_datasheets (
    id VARCHAR(100) PRIMARY KEY DEFAULT uuid_generate_v4()::VARCHAR,
    passport_id VARCHAR(100) NOT NULL,
    definition_id VARCHAR(100) NOT NULL,
    data_category VARCHAR(20),
    data JSONB,
    created_by_id VARCHAR(255),
    created_by JSONB NOT NULL,
    created_time TIMESTAMP(6) WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_passport_datasheets_passport
        FOREIGN KEY (passport_id) REFERENCES public.passports (id) ON DELETE CASCADE,
    CONSTRAINT fk_passport_datasheets_definition
        FOREIGN KEY (definition_id) REFERENCES public.datasheet_definitions (id),
    CONSTRAINT uq_passport_datasheet UNIQUE (passport_id, definition_id)
);

CREATE INDEX IF NOT EXISTS idx_passport_datasheets_passport_id
    ON public.passport_datasheets(passport_id);
CREATE INDEX IF NOT EXISTS idx_passport_datasheets_definition_id
    ON public.passport_datasheets(definition_id);

COMMIT;
