-- ADR 0004: Normalized global datasheet definitions.
--
-- The legacy model copied a complete datasheet, including every property row, into each
-- passport built from the same dictionary URI. This migration splits the immutable,
-- URI-keyed definition (shared globally and cached locally) from the per-passport instance,
-- which only carries the passport-specific values.
--
-- Forward-only: the legacy tables are dropped and their rows are not migrated.
--
-- `passport_datasheets.created_by_id` and `created_by` keep the administrator snapshot used by
-- the current authentication model. A later migration re-links `created_by_id` to `api_keys(id)`
-- and drops the snapshot once machine credentials author operational records.

-- 1) Drop the legacy per-passport datasheet structures (FK order: mappings -> properties -> datasheets).
DROP TABLE IF EXISTS public.passport_datasheet_mappings;
DROP TABLE IF EXISTS public.datasheet_properties;
DROP TABLE IF EXISTS public.datasheet_property;
DROP TABLE IF EXISTS public.datasheets;

-- 2) Global catalogue of datasheet definitions, one row per dictionary classification URI.
CREATE TABLE public.datasheet_definitions (
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

CREATE INDEX idx_datasheet_definitions_code ON public.datasheet_definitions(code);
CREATE INDEX idx_datasheet_definitions_dictionary ON public.datasheet_definitions(dictionary);

-- 3) Property schemas belonging to a global datasheet definition.
CREATE TABLE public.datasheet_definition_properties (
    id VARCHAR(100) PRIMARY KEY DEFAULT uuid_generate_v4()::VARCHAR,
    definition_id VARCHAR(100) NOT NULL,
    code TEXT,
    platform_id TEXT,
    group_tag TEXT,
    property_type TEXT,
    definition JSONB,
    CONSTRAINT fk_definition_property_definition
        FOREIGN KEY (definition_id) REFERENCES public.datasheet_definitions (id) ON DELETE CASCADE,
    CONSTRAINT uq_definition_property_code UNIQUE (definition_id, code)
);

CREATE INDEX idx_definition_property_definition_id
    ON public.datasheet_definition_properties(definition_id);
CREATE INDEX idx_definition_property_code
    ON public.datasheet_definition_properties(code);

-- 4) Per-passport datasheet instance: binds a passport to a shared definition and holds the
--    passport-specific values. Absorbs the legacy passport_datasheet_mappings table.
CREATE TABLE public.passport_datasheets (
    id VARCHAR(100) PRIMARY KEY DEFAULT uuid_generate_v4()::VARCHAR,
    passport_id VARCHAR(100) NOT NULL,
    definition_id VARCHAR(100) NOT NULL,
    data_category VARCHAR(20),
    data JSONB,
    created_by_id TEXT,
    created_by JSONB NOT NULL,
    created_time TIMESTAMP(6) WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_passport_datasheets_passport
        FOREIGN KEY (passport_id) REFERENCES public.passports (id) ON DELETE CASCADE,
    CONSTRAINT fk_passport_datasheets_definition
        FOREIGN KEY (definition_id) REFERENCES public.datasheet_definitions (id) ON DELETE RESTRICT,
    CONSTRAINT uq_passport_datasheet UNIQUE (passport_id, definition_id)
);

CREATE INDEX idx_passport_datasheets_passport_id
    ON public.passport_datasheets(passport_id);
CREATE INDEX idx_passport_datasheets_definition_id
    ON public.passport_datasheets(definition_id);
