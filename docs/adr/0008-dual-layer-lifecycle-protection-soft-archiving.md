# ADR 0008: Dual-Layer Deletion Protection with Soft Archiving

## Status
accepted

## Context and Decision
We implemented production service-level soft archiving (`status = 'ARCHIVED'` and `archived_time`) for `projects` and `passports` with default query filtering (`WHERE status != 'ARCHIVED'`), while preserving PostgreSQL schema-level `ON DELETE CASCADE` foreign keys. We decided this because circular economy records carry 30–100 year legal and regulatory lifespans under EU Digital Product Passport mandates where accidental permanent deletion via administrative action would cause catastrophic data loss, while automated test suites and ephemeral environments require clean, instant database teardowns without manual cleanup logic.

## Considered Options
- *Database-only hard cascade deletes*: Executing hard SQL `DELETE` in production. Rejected because a single mistaken API call or compromised administrative token permanently and irreversibly erases entire multi-year passport trees, verified environmental declarations, and historical audit logs.
- *Removing DDL cascades entirely*: Stripping database cascade constraints and enforcing soft deletion exclusively. Rejected because automated integration tests (Testcontainers, JUnit) become burdened with complex multi-step cleanup logic and risk leaking test state.

## Consequences
Production data cannot be permanently purged through standard API endpoints, and archived projects or passports can be audited or restored via explicit query filters (`?include_archived=true`). Ephemeral testing environments maintain maximum developer velocity through clean physical database cascade deletions.
