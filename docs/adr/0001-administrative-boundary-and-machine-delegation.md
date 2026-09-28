# ADR 0001: Administrative Boundary and Machine Delegation

## Status
accepted

## Context and Decision
Human administrators exclusively govern infrastructure (`owners`, `projects`, `access_groups`), while all operational domain interactions with passports and datasheets are performed by third-party applications using machine API keys scoped under project access groups. We renamed the `users` table to `admins` and removed user roles, as all interactive console users are administrators. We decided this because material passports represent machine-generated building models (BIM exports, sensor logs, and LCA audits) where human administrative interfaces should not introduce unvetted domain mutations or synthetic user personas.

## Considered Options
- *Polymorphic user and machine domain access*: Allowing human admins to directly create and edit child passports and datasheets in the web UI. Rejected because it blurs accountability, complicates audit logs, and contradicts the domain reality that passports are authored by external engineering software.
- *Retaining `Role.USER`*: Keeping non-admin user accounts without project ownership. Rejected because standard end-users have no direct role or data ownership in the passport manager console.

## Consequences
Administrative UI workflows remain simple and decoupled from domain passport schemas. All passport mutations carry authentic machine credential attribution without ghost user accounts.
