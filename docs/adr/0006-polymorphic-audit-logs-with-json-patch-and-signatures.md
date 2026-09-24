# ADR 0006: Polymorphic Audit Logs with RFC 6902 JSON Patch and Cryptographic Signatures

## Status
accepted

## Context and Decision
We refactored passport-only logging into a unified `audit_logs` subsystem tracking mutations across `passports`, `passport_datasheets`, and `datasheet_definitions` via polymorphic target columns (`target_type`, `target_id`), recording state changes as RFC 6902 JSON Patch arrays (`[{"op": "replace", "path": "...", "value": ...}]`). In addition, we introduced `audit_log_tags` to record validation state transitions (`PENDING`, `APPROVED`, `REJECTED`) accompanied by an HMAC-SHA256 cryptographic signature computed over a canonical composite payload (`log_id:target_id:email:tag:sha256(diff)`). We decided this to unify mutation auditing across all domain entities, provide standardized bidirectional diff tooling, enable $O(1)$ project compliance feed queries via denormalized `project_id` and `passport_id` index columns, and guarantee tamper-evident proof that a validated state has not been altered post-hoc.

## Considered Options
- *Dedicated foreign keys and tables per audited entity*: Defining separate log tables (`passport_logs`, `datasheet_logs`, etc.) or sparse columns. Rejected because it proliferates database tables and repository layers without providing better operational querying.
- *Full before-and-after JSON snapshots*: Storing complete entity payloads on every update. Rejected because it incurs massive storage bloat on minor property modifications compared to compact RFC 6902 JSON Patch operations.
- *Unsigned validation metadata*: Storing validation status as plain text without cryptographic binding. Rejected because it allows silent tampering with audit histories and fails circular economy certification standards.

## Consequences
Audit feeds for an entire project or passport can be queried in $O(1)$ indexed time without multi-table joins. Any post-hoc modification to the entity diff, validator email, or tag status mathematically invalidates the HMAC signature.
