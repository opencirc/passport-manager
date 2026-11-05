# ADR 0002: Direct Project Scoping on Passports with Partial Unique Root Index

## Status
accepted

## Context and Decision
We placed `project_id` directly on all `passports` records (`ON DELETE CASCADE`) and enforced the 1:1 project-to-root passport invariant via a PostgreSQL partial unique index on `(project_id) WHERE parent_id IS NULL`. We decided this instead of storing `root_passport_id` on `projects` or dynamically traversing the parent hierarchy from child passports to root, because it enables instantaneous $O(1)$ indexed authorization checks and project-level queries while eliminating circular foreign key deadlocks upon creation and deletion.

## Considered Options
- *Storing `root_passport_id` on `projects`*: Inverts foreign key cascade semantics (deleting the root would delete the project, rather than the project deleting its passports) and introduces circular foreign key dependencies during record creation.
- *Dynamic hierarchy traversal without `project_id` on children*: Omitting `project_id` on child passports avoids column denormalization, but forces recursive CTE tree traversals on every authorization check and breaks efficient indexed project-wide pagination across tens of thousands of building components.

## Consequences
Authorizing API key operations against passports is an instantaneous $O(1)$ indexed comparison (`passport.project_id == apiKey.projectId`). Cross-project reparenting requires updating `project_id`, an acceptable trade-off for a rare operational event in physical building lifecycles.
