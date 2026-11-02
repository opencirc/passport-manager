-- ADR 0007: Pruning passport templates and lifecycle tracking.
--
-- External data dictionaries (bSDD, Lexicon) define the schemas for material components,
-- which makes the internal passport template catalogue redundant. Lifecycle milestones are
-- consolidated into the unified audit log introduced by a later migration, so the separate
-- lifecycle table is removed as well.
--
-- Forward-only: rows in these tables are not migrated. Indexes are dropped with their tables.

DROP TABLE IF EXISTS public.passport_lifecycles;
DROP TABLE IF EXISTS public.passport_templates;
