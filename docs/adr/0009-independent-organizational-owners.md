# ADR 0009: Independent Organizational Owners

## Status
accepted

## Context and Decision
We introduced an independent `owners` entity with an optional 1:N relationship to `projects` (`projects.owner_id ON DELETE SET NULL`), deliberately omitting user membership, contact details, or multi-tenant boundary logic for now. We decided this to establish a clean organizational entity in the schema for future client and building-owner tenancy without prematurely coupling administrative accounts or imposing multi-tenant query filters while global administrators manage all projects.

## Considered Options
- *Immediate user-to-owner multi-tenancy*: Linking administrators directly to owners (`admins.owner_id`). Rejected as premature complexity for the current milestone since global administrators oversee all projects and client self-service is not yet required.
- *Inline string attribute on projects (`owner_name`)*: Omitting the `owners` table entirely. Rejected because a dedicated entity provides a stable foreign key anchor that can be extended with billing, contacts, and tenancy boundaries without breaking migrations later.

## Consequences
Projects can optionally reference an owner organization. Deleting an owner nullifies the reference on projects (`ON DELETE SET NULL`), preventing unintended cascading destruction of project assets.
