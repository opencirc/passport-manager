# ADR 0005: Structured API Key Lookup with Soft Revocation

## Status
accepted

## Context and Decision
We structured machine API keys with an unhashed prefix format (`opc_live_<key_id>_<secret>`) for $O(1)$ indexed key lookups verified against a constant-time SHA-256 hash, and adopted soft revocation (`status = 'REVOKED'`) alongside `ON DELETE SET NULL` on operational references instead of database-level `ON DELETE RESTRICT`. We decided this because slow salted password hashing (BCrypt) saturates CPU under high-throughput automated ingestion pipelines, and database-level `RESTRICT` constraints abort cascade deletions when decommissioning projects whose keys authored historic records.

## Considered Options
- *Salted password hashing (BCrypt)*: Standard password hashing consumes 50–100ms of CPU per evaluation. At ingestion loads of dozens of requests per second from automated BIM pipelines, CPU cores become saturated solely running hash loops.
- *Relational restrict constraints (`ON DELETE RESTRICT`)*: Prevented deleting API keys at the database level once they authored operational records, but caused cascade deletion transactions on projects to fail with foreign key violation deadlocks.

## Consequences
API key lookup and verification executes in under 0.05ms with zero vulnerability to timing attacks when compared in constant time. Key rotation relies on application-level status deactivation (`REVOKED`), immediately revoking access while preserving audit trail integrity and allowing clean project decommissioning.
