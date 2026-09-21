-- Convert remaining VARCHAR columns that could potentially exceed their length to TEXT

-- Datasheets table
ALTER TABLE public.datasheets ALTER COLUMN created_by_id TYPE TEXT;

-- Datasheet Properties table
ALTER TABLE public.datasheet_properties ALTER COLUMN code TYPE TEXT;
ALTER TABLE public.datasheet_properties ALTER COLUMN property_type TYPE TEXT;

-- Passports table
ALTER TABLE public.passports ALTER COLUMN name TYPE TEXT;
ALTER TABLE public.passports ALTER COLUMN created_by_id TYPE TEXT;

-- Passport Templates table
ALTER TABLE public.passport_templates ALTER COLUMN name TYPE TEXT;
ALTER TABLE public.passport_templates ALTER COLUMN created_by_id TYPE TEXT;

-- Passport Logs table
ALTER TABLE public.passport_logs ALTER COLUMN created_by_id TYPE TEXT;

-- Passport Lifecycles table
ALTER TABLE public.passport_lifecycles ALTER COLUMN created_by_id TYPE TEXT;
ALTER TABLE public.passport_lifecycles ALTER COLUMN event_type TYPE TEXT;
