# ADR 0007: Pruning Passport Templates and Lifecycle Tracking

## Status
accepted

## Context and Decision
We removed `passport_templates` and `passport_lifecycles` from the core application data model. We decided this because external standardized classification dictionaries (bSDD, Lexicon) provide the definitive schemas for material components, making internal templates redundant dead weight, and lifecycle events (inspections, deconstruction, reuse milestones) are consolidated directly into `audit_logs` without dual-table write amplification.

## Considered Options
- *Rebinding `passport_templates` to projects and API keys*: Adding `project_id` and machine authorship to templates. Rejected because third-party BIM and engineering tools already manage component schemas via standard buildingSMART data dictionaries, making an internal template engine an unnecessary maintenance burden.
- *Maintaining separate `passport_lifecycles` tables*: Keeping separate tables for diff mutations versus lifecycle milestones. Rejected because having two divergent logging models created schema inconsistencies, duplicated API endpoints, and fragmented the chronological history of a material passport.

## Consequences
Reduces codebase and database complexity by two complete entity lifecycles, associated repository layers, and migration scripts. All domain history flows through a single unified audit stream.
