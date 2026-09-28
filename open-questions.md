# The Disconnect between passport_templates and Administrative Boundaries [RESOLVED - PRUNED]
## Resolution
`passport_templates` has been pruned from the target data model (adopting Option B). External platforms and data dictionaries (bSDD, Lexicon) define standardized classification schemas and property structures directly via datasheets, rendering internal template entities redundant dead weight. Removing `passport_templates` eliminates an entire table, avoids administrative bottlenecks, and eliminates foreign key impedance mismatches.

---

# Coarse CRUD Permissions vs. Circular Economy Lifecycles
## The Friction
access_groups.permissions defines four boolean flags:
```
{
    "canCreate": true,
    "canRead": true,
    "canUpdate": true,
    "canDelete": false
}
```
## The Operational Consequence
In construction circular economy workflows, third-party actors require distinct, asymmetric capabilities:

• BIM Exporter: Needs to create child passports and attach initial material datasheets. Must not be able to alter passport statuses or erase lifecycle records.
• LCA / EPD Auditor: Needs to append environmental properties and validation logs. Must not have permission to restructure the physical assembly hierarchy (parent_id).
• Demolition / Re-use Contractor: Needs to append inspection or deconstruction records to audit_logs and toggle passport status (ACTIVE -> INACTIVE), but must not be granted generic update/delete rights over material engineering specs.
With only four coarse flags, enabling a contractor to log a milestone requires granting broad canCreate or canUpdate permissions, exposing core passport engineering structures to accidental corruption.

## High-Utility Solution
Refine access_groups.permissions into action-oriented domain capabilities:
```
{
"passport": { "createChild": true, "updateMetadata": true, "changeStatus": false },
"datasheet": { "attach": true, "detach": false },
"log": { "appendEvent": true }
}
```
This restricts third-party integrations to their exact functional role, minimizing blast radius while preserving the simple JSONB storage format.

---

# Catastrophic Data Loss Risk from Hard Cascading Deletes [RESOLVED - SOFT ARCHIVING]
## Resolution
Resolved via a Dual-Layer Deletion Protection Strategy integrated into `59-new-data-model.md`:
• **Production Soft Archiving**: Production application services enforce soft archiving rather than issuing SQL `DELETE` operations. Deleting a project sets `projects.status = 'ARCHIVED'` and records `projects.archived_time = NOW()`, automatically propagating down the passport tree (`passports.status = 'ARCHIVED'`, `passports.archived_time = NOW()`). REST endpoints exclude archived entities by default, while supporting administrative queries via `?include_archived=true` for regulatory compliance auditing, legal verification, or project restoration (`status = 'ACTIVE'`).
• **Preserved DDL Cascades**: Foreign key `ON DELETE CASCADE` constraints remain defined in the PostgreSQL database schema strictly to allow clean, instant, automated database teardowns in automated testing suites (Testcontainers, JUnit) and ephemeral development environments without requiring manual multi-step cleanup logic.

---

# The Low Net Utility of the owners Table
## The Friction
owners contains only id, name, and created_time. It has no relationships to admins, no user accounts, no contact or billing information, and no authorization boundaries.

## The Operational Consequence
• owners currently operates as a standalone lookup table for an organization name string.
• Developers must maintain an extra table, migration scripts, repository interfaces, controller endpoints, and foreign key joins on projects.owner_id, yet it provides zero access control or tenancy value today (global
administrators still oversee all projects).

## High-Utility Solution
• Option A (Simplify to Zero Friction): If multi-tenant administrative isolation is not immediately required, replace projects.owner_id with a simple owner_name TEXT column on projects. This delivers the identical display
utility while eliminating an entire table, reducing database joins, and speeding up project creation.
• Option B (Deliver True Tenancy Value): If organizational boundaries are needed, link admins to owners (admins.owner_id -> owners(id)). This turns owners into a functional tenant boundary, allowing client admins to manage
only their organization's projects while super-admins retain global visibility.

---

# Project-Wide Audit Query Inefficiency [RESOLVED - DENORMALIZED]
## Resolution
Resolved in the `audit_logs` refactor. `audit_logs` denormalizes `project_id` and `passport_id` directly onto the log row (`project_id REFERENCES projects(id) ON DELETE CASCADE`, `passport_id REFERENCES passports(id) ON DELETE CASCADE`) with composite indexes on `(project_id, created_time DESC)` and `(passport_id, created_time DESC)`. This delivers instant $O(1)$ chronological range scans for project-wide and passport-wide audit trails without multi-table join overhead.

---