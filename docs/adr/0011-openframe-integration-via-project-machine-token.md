# ADR 0011: openframe Integration Through a Per-Project Machine Token

## Status
proposed (2026-11-04)

## Context and Decision
openframe reaches this API only through its backend passthrough (`/projects/{projectId}/integrations/opencirc/passthrough/*`), which forwards browser requests to the OpenCirc base URL and spreads the openframe project-integration configuration JSON as upstream headers. The browser itself sends no OpenCirc credentials. Under the access hierarchy, the only credential that may operate on passports is an API key scoped to one OpenCirc project (ADR 0001, 0002).

We decided that one openframe project maps to exactly one OpenCirc project, and that the mapping is carried by a single machine token:

1. An operator provisions, in the admin interface (ADR 0010), an OpenCirc project with its root passport, an access group with the permissions openframe needs, and an API key under that group.
2. The token is stored in openframe's project-integration configuration for that openframe project and sent as the `X-Api-Key` header by the passthrough. The first implementation stores it as `{"X-Api-Key": "opc_live_…"}`; the follow-up is a typed `apiKey` field in openframe-api's OpenCirc configuration schema that maps to the header, plus a CLI command to set it.
3. openframe never holds an administrator session. Provisioning stays an operator step until self-provisioning is wanted, in which case openframe-api would call the administrator surface with its own administrator account.

The Building Log in openframe-frontend creates every passport under the project's root passport, which it obtains as the single entry of `GET /passport/root` for its token. It no longer creates parentless passports, no longer shows the creator's name, and drops the template screens; the passport log tab can later read the audit feed.

## Considered Options
- *Administrator credentials in openframe*: Would let openframe provision projects itself today. Rejected because it breaks the administrative boundary (ADR 0001) and exposes every project behind one credential.
- *Many OpenCirc projects per openframe project*: Rejected for now; the Building Log has one tree per building and the root passport is that building.
- *Key per openframe user*: Rejected; the passthrough is server-to-server and the access group, not the user, is the unit of permission.

## Consequences
- `@opencirc/api-tools` must be released with the new passport schemas before openframe-frontend can parse any response; this is the first dependency on the openframe side.
- Revoking the key or archiving the OpenCirc project cuts openframe off with `401`, which is the intended kill switch.
- Audit log entries created through openframe are attributed to the project's key, not to the openframe user. Per-user attribution would require openframe to pass a validator identity, which the audit log tag already models (`email`).
