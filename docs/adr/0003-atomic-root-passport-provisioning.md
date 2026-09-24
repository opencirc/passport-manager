# ADR 0003: Atomic Root Passport Provisioning

## Status
accepted

## Context and Decision
We decided to provision each project's root passport (`parent_id = NULL`, `created_by_id = NULL`) atomically within the `POST /api/projects` database transaction. We decided this rather than requiring an asynchronous secondary API call authenticated via a bootstrap API key because it guarantees that no project ever exists without its required root anchor and eliminates synthetic credential generation ceremonies.

## Considered Options
- *Synthetic bootstrap API key ceremony*: Auto-generating an access group and synthetic API key upon project creation to invoke a secondary passport creation endpoint. Rejected because network timeouts, process restarts, or schema validation failures leave empty, corrupted projects with orphaned credentials, and fake key attribution dilutes audit logs.
- *Two-phase deferred initialization*: Allowing projects to exist indefinitely without a root passport. Rejected because downstream API key integrations require a root passport anchor to build assembly component trees.

## Consequences
Root passports represent project infrastructure provisioned by administrative action with `created_by_id = NULL`. Child passports, property attachments, and lifecycle mutations remain strictly authored by third-party integrations via API keys.
