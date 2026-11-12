# ADR 0010: Admin Interface as Operator Console and End-to-End Test Surface

## Status
proposed (2026-11-04, revised 2026-11-10)

## Context and Decision
The access hierarchy (ADR 0001–0009) splits the API into an administrator surface (`/api/auth`, `/api/admin/**`, JWT cookie session) and a machine surface (`/api/passport/**`, `/api/audit-logs/**`, API key bearer token). Provisioning a project, its root passport, an access group and an API key is therefore an administrator task that must happen before any integration, including openframe, can touch a passport. Swagger exposes these endpoints but is text only and gives no way to exercise the full lifecycle visually.

We decided that the existing `opencirc/admin-interface` repository (Vite, React, react-query, `@opencirc/api-tools`) becomes the operator console for this API and the primary end-to-end test surface. It is continued from its current state (login, dashboard, root passport viewer) rather than started over, and it is extended with:

- Owners and projects: create, list, edit, archive, restore, show the root passport id.
- Access groups per project with the four CRUD permission flags, listed under their project.
- API keys per access group: create (token shown once), list, revoke.
- An audit log view: the administrator selects a project and sees, read-only, its root passport, the passport tree, each passport's datasheet values and the audit trail with its validation tags.

The audit log view is served by read-only administrator endpoints under `/api/admin/projects/{projectId}` (`root-passport`, `passports/{id}`, `passports/{id}/children`, `audit-logs`, `passports/{id}/audit-logs`). ADR 0001 keeps administrators from authoring passport data; it does not keep them from seeing what the machine integrations did, and oversight of a project is an administrative duty. No administrator endpoint creates, updates, archives or tags a passport, a datasheet or an audit entry. Validation tags are signed with the secret of the key that tagged, so the administrator view shows a tag and its signer but cannot re-verify it.

The console talks to the API directly on the same host (`EXPOSED_API_URL`), so the admin cookie is sent by the browser without any proxy.

## Considered Options
- *Swagger UI only*: Already available. Rejected as the test surface because it cannot carry a lifecycle across endpoints, hides the token-once flow and gives no visual feedback.
- *A machine console that acts as an API key*: The first draft of this ADR had the operator paste a machine token and the console call the machine surface as that key. Rejected because every administrator can mint a token anyway, so the step added no protection; it only bloated the key list with tokens issued to look at logs and made oversight depend on possessing a live credential. Machine behaviour is verified by the backend lifecycle test and the CLI, with the same token an integration uses.
- *New admin app inside the passport-manager repository, served by Spring Boot under `/admin`*: Same origin and no CORS. Rejected because an operator UI already exists with the shared toolchain, design and deployment pipeline; a second one would split effort.
- *Admin screens inside openframe-frontend*: Rejected because the audience differs. openframe project users consume passports; OpenCirc operators govern projects and keys. Mixing them would also require handing openframe an administrator session.

## Consequences
- `@opencirc/api-tools` must gain the administrator routes (`auth`, `owners`, `projects`, `access-groups`, `api-keys`) and the project inspection routes, and its passport schemas must follow the current DTOs (no `createdBy`, nullable `createdById`, `projectId`, `archivedTime`, `archived` status). The admin interface pins that release.
- The admin interface holds no machine token and never calls the machine surface. Creating passports, writing datasheet values, archiving passports and tagging audit entries stay with integrations and the CLI.
- The removed template routes and the root passport viewer's assumption that any passport can be a root are deleted from the admin interface.
- A fresh deployment needs a first administrator before the console can log in. Until a bootstrap mechanism exists (see ADR 0011) that is the `register-admin` shell command.
- Testing the openframe path no longer blocks on openframe: the lifecycle test exercises every machine-side behaviour with the same token openframe would use, and the operator sees the result in the audit log.
