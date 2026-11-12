# ADR 0012: Passwordless Administrator Login by Email Link

## Status
proposed (2026-11-06), awaiting CTO approval; amends the administrator authentication of ADR 0001

## Context and Decision
Administrators currently authenticate with email and password (ADR 0001) and can only be created from the shell with `register-admin`, which neither scales to a second operator nor to environments nobody shells into. Password handling also brought the operational problems seen during local testing: stored typos in password managers, stale cookies, and no self-service way to add or remove an operator.

We decided to replace the password with a single-use login link sent by email, and to manage administrators from the admin interface:

1. **Request a link.** `POST /api/auth/login-link` with `{ "email" }` always answers `202 Accepted`, whether or not the email belongs to an active administrator, so the endpoint reveals nothing about accounts. For an active administrator a 256-bit random token is generated, only its SHA-256 hash is stored in a new `admin_login_links` table (`id`, `admin_id`, `token_hash`, `expires_time` = now + 15 minutes, `used_time`, `created_time`), and the raw token is sent in a link to the admin interface: `<admin-ui>/auth/link?token=<raw>`.
2. **Redeem the link.** The admin interface posts the token to `POST /api/auth/login-link/redeem`. The API looks the hash up, requires the link to be unexpired and unused, marks it used, and issues the same session cookies as the password login does today (ADR 0001, JWT access and refresh cookies). The response is the `AdminDto`. Every other link of that administrator is invalidated at the same time.
3. **Invite and manage administrators.** New endpoints under `/api/admin/admins`: list, invite (`{ email, firstName, lastName }` creates an inactive-until-first-login administrator and sends a login link), resend link, deactivate, reactivate. The admin interface gets an **Administrators** menu item backed by them. An administrator cannot deactivate the last active administrator or themselves.
4. **Bootstrap.** The first administrator of a fresh environment is still created from the shell (`register-admin` without a password, or the seeder); from then on everything happens through invitations.
5. **Email delivery.** Through Spring Mail over SMTP (SES, Resend, Postmark or any SMTP relay), configured by environment (`MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_FROM`, `ADMIN_UI_BASE_URL`). Without a mail host configured the API logs the link instead of sending it, which is what local development and the end-to-end tests use.
6. **Password login is removed** once the link login is live: the `password` column and `register-admin --password` go in a follow-up migration, so the two never coexist in production for long.

## Considered Options
- *Keep passwords and add an Administrators tab*: Least change, but keeps a credential store to protect, password rules to enforce, and the operational issues above. Rejected.
- *Single sign-on with a workspace identity provider (Google Workspace, Microsoft Entra)*: Least code and inherits MFA, but ties every operator, including external consultants and client administrators (ADR 0009 anticipates owner tenancy), to one workspace. Rejected for now; it can be added later as an alternative way to obtain a session.
- *Passkeys (WebAuthn)*: Strongest and provider independent, but needs an invitation mechanism anyway for the first registration. Deferred: the email link is that mechanism, and passkeys can be layered on it in a later ADR.

## Consequences
- The login page becomes a single email field with "Send me a login link" and a "check your inbox" state; a new route `/auth/link` redeems the token and lands on the dashboard. Expired or used links show one generic message and offer to send a new one.
- Rate limiting of link requests per email and per address, and the session lifetimes, remain part of the hardening work owned by the CTO; the link endpoint is designed so that a limiter can sit in front of it without changing the flow.
- Links are single use, expire in 15 minutes, are hashed at rest, and are not bound to the requesting address so a link opened on another device still works.
- Machine authentication (API keys) is untouched. The openframe passthrough never sees administrator sessions.
- Schema change: migration V11 adds `admin_login_links` and makes `admins.password` nullable; a later migration removes it.
- New dependency: `spring-boot-starter-mail`. New configuration: the mail variables and `ADMIN_UI_BASE_URL`, needed in staging and production before this ships.
- The audit of administrator actions (who invited or deactivated whom) is out of scope here and should follow the audit log pattern of ADR 0006 when administrators become auditable.
