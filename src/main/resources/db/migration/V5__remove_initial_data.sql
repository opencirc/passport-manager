-- Removes the seed data inserted by V2__insert_initial_data.sql
-- (safe to run even if some rows were already removed)

BEGIN;

-- 1) Remove mappings first (FKs exist, but doing it explicitly keeps intent clear)
DELETE FROM public.passport_datasheet_mappings pdm
WHERE pdm.passport_id IN (
    SELECT p.id
    FROM public.passports p
    WHERE p.created_by_id = '717753dc-ba6c-4a8d-87c9-cce878986553'
)
   OR pdm.datasheet_id IN (
    SELECT d.id
    FROM public.datasheets d
    WHERE d.created_by_id = '717753dc-ba6c-4a8d-87c9-cce878986553'
);

-- 2) Remove passports created by the seeded admin
DELETE FROM public.passports p
WHERE p.created_by_id = '717753dc-ba6c-4a8d-87c9-cce878986553';

-- 3) Remove datasheets created by the seeded admin
-- (datasheet_properties will cascade due to FK ON DELETE CASCADE)
DELETE FROM public.datasheets d
WHERE d.created_by_id = '717753dc-ba6c-4a8d-87c9-cce878986553';

-- 4) Remove the seeded users
-- (api_keys will cascade due to FK ON DELETE CASCADE)
DELETE FROM public.users u
WHERE u.email IN ('admin@test.com', 'user@test.com');

COMMIT;
