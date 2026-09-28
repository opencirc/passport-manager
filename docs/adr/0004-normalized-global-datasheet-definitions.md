# ADR 0004: Normalized Global Datasheet Definitions

## Status
accepted

## Context and Decision
We normalized the datasheet subsystem by separating external data dictionary standards into an immutable, shared system catalog (`datasheet_definitions` and `datasheet_definition_properties`) cached by classification URI (`platform_id`), and storing passport-specific property assignments in `passport_datasheets` owned directly by `passports` (`ON DELETE CASCADE`). We decided this because the legacy model duplicated complete datasheet and property rows per passport, causing combinatorial storage bloat, repeated external HTTP calls to buildingSMART APIs, and cross-project cascade deadlocks during project decommissioning.

## Considered Options
- *Duplicating full datasheet definitions per passport (legacy model)*: Duplicating all property schemas for every passport instance created millions of redundant rows (e.g. 250,000 property rows for 5,000 passports referencing `IfcWall`) and incurred heavy external API ingestion latency.
- *Project-scoped mutable datasheets*: Storing dictionary definitions under individual projects with API key creator foreign keys. Rejected because cross-project sharing of standard industry schemas caused relational locking and cascade aborts when deleting projects.

## Consequences
External classifications (bSDD, Lexicon) are fetched once and cached globally, enabling instant local property resolution with zero external HTTP latency on repeated ingestion. Instance data in `passport_datasheets` cascades cleanly with passports upon project deletion without leaving orphaned records or impacting other projects.
