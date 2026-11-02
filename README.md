<img alt="OpenCirc" src="https://opencirc-public.s3.fr-par.scw.cloud/opencirc-logo.png" width="200" />

**Construction Materials Passport Generator and Manager**

  This application lets administrators set up projects and machine integrations, and lets those integrations generate and manage passports for construction materials through a REST API. It uses external dictionaries such as bsDD (https://search.bsdd.buildingsmart.org/) and lexicon (https://definehub.com/en/) to generate passports in the defined templates.

This README will guide you through the steps to clone the code, set up the application on your local machine, and use the API to generate passports.

**1. Prerequisites**
Before getting started, ensure you have the following software installed on your system:

- Git (https://git-scm.com/downloads)
- Docker (https://docs.docker.com/get-started/get-docker/)
- Docker Compose (https://docs.docker.com/compose/install/)


**2. Clone the Repository**

Open your terminal and clone the project:

    git clone https://github.com/opencirc/passport-manager.git


Navigate into the project directory:

    cd passport-manager

**3. Set Up and Run the Application**

The application can be started easily using Docker Compose.

1. Ensure Docker and Docker Compose are installed on your machine. 

2. In the project root directory, run:

```bash
docker-compose up --build -d
```
   
This command will:

- Build the Docker images 
- Start the necessary containers 
- Initialize the database
- Make the REST API available at http://localhost:8080

If you want to access the shell, run:

```bash
docker compose --profile shell run --rm shell
```

3. Once the containers are up and running, you should see logs in the terminal indicating that the application has started successfully.

**4. Accessing the API**

Swagger UI (Interactive API Documentation):

	http://localhost:8080/swagger-ui/index.html

Base API URL

	http://localhost:8080/

**5. Access Model**

The API has two kinds of actors, and they never share endpoints:

| Actor | Authenticates with | Works on |
| --- | --- | --- |
| **Administrator** (`admins`) | Email and password, then JWT cookies | Owners, projects, access groups and API keys under `/api/admin/**` |
| **Machine integration** (`api_keys`) | A bearer token issued under an access group | Passports, datasheets and audit logs under `/api/passport/**` and `/api/audit-logs/**` |

Every project is created together with its **root passport**, the anchor all machine-authored child passports hang from. An **access group** belongs to one project and holds the CRUD permissions (`canCreate`, `canRead`, `canUpdate`, `canDelete`) of the API keys issued under it. Each API key is therefore scoped to exactly one project: it can only read and change passports of that project, and the HTTP method decides which permission applies (GET → read, POST → create, PUT → update, DELETE → delete).

Deleting a project through the API soft-archives it and every passport in it (`status = archived`); nothing is physically removed. Archived records are hidden from default listings and can be shown with `?include_archived=true`. Every mutation of a passport, a passport datasheet or a datasheet definition is recorded in the audit log as an RFC 6902 JSON Patch, and an audit log entry can be approved or rejected with a signed validation tag.

**5.1 Administrator Login**

Log in with an administrator account (see 6.1 for creating one; the seeder creates `admin@test.com` with the configured seed password):

```bash
POST /api/auth/login
{
  "email": "admin@test.com",
  "password": "Password123!"
}
```

The response sets the `access_token` and `refresh_token` cookies, which authenticate all `/api/admin/**` calls. The usual flow is:

```bash
POST /api/admin/owners                                  # optional: { "name": "Owner Org" }
POST /api/admin/projects                                # { "name": "Building B", "description": "...", "ownerId": "<optional>" }
POST /api/admin/projects/{projectId}/access-groups      # { "name": "BIM pipeline", "permissions": { "canCreate": true, "canRead": true, "canUpdate": true, "canDelete": false } }
POST /api/admin/access-groups/{groupId}/api-keys        # { "name": "Revit production", "expirationTime": "2027-12-31T23:59:59Z" }
```

The project response carries `rootPassportId`. The API key response carries the `token` exactly once; only its SHA-256 hash is stored. Revoke a key with `POST /api/admin/api-keys/{id}/revoke`.

**5.2 Machine Requests**

Send the token as a bearer token (or in the `X-Api-Key` header):

```bash
Authorization: Bearer opc_live_<key_id>_<secret>
```

The token is resolved with one indexed lookup on `<key_id>` and a constant-time comparison of the hashed `<secret>`. Revoked or expired keys, and keys of archived projects, are rejected with `401`. Child passports are created under a parent of the same project:

```bash
POST /api/passport/platform/bsdd
{
  "name": "Wall W-01",
  "parentId": "<rootPassportId or any passport of the project>",
  "platformId": "https://identifier.buildingsmart.org/uri/buildingsmart/ifc/4.3/class/IfcWall",
  "dataCategory": "generic"
}
```

Datasheet definitions fetched from bSDD are cached globally, so the same classification is fetched once and reused by every passport. `DELETE /api/passport/{id}` soft-archives the passport and its subtree.

**5.3 Audit Logs and Validation**

```bash
GET  /api/audit-logs/project/{projectId}            # project-wide stream, newest first
GET  /api/audit-logs/passport/{passportId}          # passport-wide stream, newest first
GET  /api/audit-logs/target/{targetType}/{targetId} # history of one PASSPORT, PASSPORT_DATASHEET or DATASHEET_DEFINITION
POST /api/audit-logs/{logId}/tags                   # { "tag": "APPROVED", "email": "lead.auditor@certifier.org" }
GET  /api/audit-logs/{logId}/tag/verify             # { "valid": true|false, ... }
```

A tag is signed with HMAC-SHA256 over `log_id:target_id:email:tag:sha256(diff)` keyed by the secret of the token that created it. Because only the hash of the secret is stored, verification has to be called with the same key's token; any later change to the diff, the validator or the state makes `valid` false.

**6. CLI Tool Commands**

This application also provides a CLI tool to manage administrators, projects, access groups, API keys, data dictionary templates, and to seed the database.

**6.1 Register an Administrator:**

```bash
register-admin --email <email> --firstName <firstName> --lastName <lastName>
```

> --password : Optional. When omitted you are prompted for it with masked input, which keeps it out of the process list and the shell history. The prompt needs an interactive terminal: when the shell's input or output is piped (for example through `tee`), the command refuses instead of reading the password from the pipe, so pass `--password <password>` in scripts. Use at least 12 characters with a mix of uppercase, lowercase, digits, or symbols.

`list-admins` lists the existing accounts.

**6.2 Owners, Projects and Access Groups:**

```bash
create-owner --name <owner name>
create-project --name <project name> [--description <text>] [--ownerId <owner id>]
list-projects [--includeArchived true]
archive-project --projectId <project id>
create-access-group --projectId <project id> --name <name> [--description <text>] \
    [--canCreate true] [--canRead true] [--canUpdate true] [--canDelete false]
```

`create-project` prints the id of the root passport provisioned with the project.

**6.3 API Keys:**

```bash
create-api-key --accessGroupId <access group id> --name <key name> [--expirationDate yyyy-MM-dd]
list-api-keys --accessGroupId <access group id>
revoke-api-key --key <key id>
```

`create-api-key` prints the full token once.

**6.4 Passports:**

```bash
create-passport-using-platform --apiKeyId <key id> --parentId <passport id> --platform bsdd \
    --platformId <classification URI> --name <passport name> [--dataCategory generic|unique]
```

The passport is created on behalf of the given key, inside its project; the key's secret is not needed from the shell.

**6.5 Data Dictionary Command:**

```bash
fetch-template --platform bsdd --code <<URI of the classification>> --raw <<true/false>>
```

> --raw : default is false

>true: returns the template without added fields

>false: returns processed templates with added fields


**6.6 Seeder Commands:**

Seed initial data into the database:

```bash
seed --type admin               # Adds the test administrator only
seed --type passportFromApi     # Adds passport data fetched from external API templates
seed --type passportFromJson    # Adds passport data from pre-saved bsdd_templates.json
seed --type all                 # Adds the administrator and passport data from bsdd_templates.json
seed                            # Defaults to all
```

The passport seeders provision a `Seed Owner`, a `Seed Project` with its root passport, a `Seed Integrator` access group with full permissions and a `Seed Key` API key, reusing them on later runs. The key's token is printed once when it is first issued.


**7.Common Issues and Troubleshooting**
	
**1. Port 8080 Already in Use.**

To Stop any process using port 8080:

```bash
sudo lsof -i :8080
sudo kill -9 <PID>
```

**2. Database Migration Fails**

Flyway migrations may fail if the database volume persists old data. Remove the volume and try again:

```bash
docker-compose down -v
docker-compose up --build
```


**8. Staging Server**

Staging runs at `https://api.staging.opencirc.org` on a single Ubuntu 24.04 box. Caddy terminates TLS and proxies to the app, which listens only on `127.0.0.1:8080` (see 8.2). The app runs as the `opencirc` user, not root: the checkout lives in `/home/opencirc/passport-manager`, the Java process runs inside a tmux session named `passport-manager`, and Postgres and Redis run in Docker (ports 5435 and 6381). If you log in as root, the home directory looks empty because nothing lives there.

**8.1 SSH Access**

SSH is closed by default. The Scaleway security group of the instance drops all inbound traffic except ports 80 and 443, so `ssh` times out until a port-22 rule for your current public IP is added. Maintainers have a local `opencirc-ssh-open staging` / `opencirc-ssh-close staging` helper pair for this (kept outside this repository on purpose); without it, add and remove the rule with the Scaleway CLI after `scw login`:

```bash
scw instance security-group create-rule security-group-id=<staging security group id> zone=fr-par-1 \
    direction=inbound action=accept protocol=TCP ip-range=<your public ip>/32 dest-port-from=22 dest-port-to=22
# ... work ...
scw instance security-group delete-rule security-group-id=<staging security group id> security-group-rule-id=<rule id> zone=fr-par-1
```

Close the port again when you are done. Deleting the rule also drops a live SSH session.

You need the staging key (`passport-manager-staging.pem`, ask a maintainer) and must log in as `opencirc`. Logging in as `ubuntu` or your local username fails with `Permission denied (publickey)` because those users do not exist. Add this to `~/.ssh/config`:

```
Host opencirc-passport-manager-staging api.staging.opencirc.org
    HostName api.staging.opencirc.org
    User opencirc
    IdentityFile ~/.opencirc/passport-manager/staging/passport-manager-staging.pem
    IdentitiesOnly yes
```

Then:

```bash
ssh opencirc-passport-manager-staging
```

**8.2 HTTPS and Reverse Proxy**

Caddy (installed from the official Caddy apt repository, `systemctl status caddy`) owns ports 80 and 443. It obtains and renews the Let's Encrypt certificate for `api.staging.opencirc.org` by itself and redirects plain HTTP to HTTPS. The whole configuration is `/etc/caddy/Caddyfile`:

```
api.staging.opencirc.org {
    reverse_proxy 127.0.0.1:8080
}
```

The app is told about the proxy through two environment variables exported in `~/passport-manager/run-host.sh` on the server, so nothing in `application.properties` is staging-specific:

```bash
export SERVER_ADDRESS="127.0.0.1"                # only Caddy talks to the app directly
export SERVER_FORWARD_HEADERS_STRATEGY="native"  # trust X-Forwarded-* from Caddy on localhost
```

With these, Spring sees the original `https` scheme and host (the OpenAPI document's server URL and the `Strict-Transport-Security` header confirm it), and port 8080 is unreachable from outside even if the security group were opened. Caddy logs go to `journalctl -u caddy`; after editing the Caddyfile run `caddy validate --config /etc/caddy/Caddyfile` and `sudo systemctl reload caddy`. `scripts/setup-staging.sh` does not install Caddy yet; on a fresh server these steps are manual.

**8.3 Shell Helpers**

The login shell loads `~/.bashrc.d/opencirc.sh` (source: `scripts/staging-shell-helpers.sh`), which provides:

| Command | What it does |
| --- | --- |
| `app-status` | One-screen overview: deployed commit, Java process, tmux sessions, Docker containers, log file |
| `app-logs` | Attach to the app's tmux session to watch live output (`Ctrl-b` then `d` to detach) |
| `app-tail` | Follow the last 200 lines of `~/logs/passport-manager.log` without attaching to tmux |
| `app-sessions` | List tmux sessions (`tmux ls`) |
| `app-redeploy` | Pull `main`, build, restart the app, and wait until it is ready |
| `app-cd` | `cd` into the checkout |

tmux is configured with mouse scrolling and a 50,000-line scrollback (source: `scripts/staging-tmux.conf`).

**8.4 Deploying**

Only `main` is deployed. Merge first, then on the server:

```bash
tmux new -s deploy     # so the build survives a dropped SSH connection
app-redeploy
```

`app-redeploy` pulls the checkout it runs from, and bash has already read the old script by then, so a change to `scripts/redeploy-app.sh` itself only takes effect on the following run. On a server whose checkout predates the current redeploy script (for example one set up before the Caddy launcher migration), pull once before the first `app-redeploy`:

```bash
app-cd && git pull --ff-only origin main
```

`app-redeploy` runs `scripts/redeploy-app.sh`: it refuses to run with uncommitted changes in the checkout, resets to `origin/main`, builds with `mvn -DskipTests package` under the memory limits in `MAVEN_OPTS`, adds the `SERVER_ADDRESS` and `SERVER_FORWARD_HEADERS_STRATEGY` exports to `run-host.sh` if a launcher from before the Caddy setup still lacks them, restarts the `passport-manager` tmux session running `run-host.sh`, and then runs `scripts/wait-for-app.sh`. That script waits up to 180 seconds for the app to listen on port 8080 (on localhost, behind Caddy) with a live Java process, and fails with a pointer to the logs otherwise. "App started" is only printed once readiness succeeds.

App output goes to both the tmux pane and `~/logs/passport-manager.log`, so logs survive restarts.

**8.5 Setting Up a Fresh Server**

`scripts/setup-staging.sh` provisions a blank Ubuntu 24.04 host end to end: packages, Docker, Temurin Java 21, Maven, swap, a GitHub deploy key, the clone, the shell helpers and tmux config, the Docker services, a first build, and the first app start. Run it as the user that should own the app (not root) and follow the prompts.

The generated `run-host.sh` already exports `SERVER_ADDRESS=127.0.0.1` and `SERVER_FORWARD_HEADERS_STRATEGY=native` (8.2), so the app comes up bound to localhost and is not reachable from outside until Caddy is installed and configured as described in 8.2. The server is not finished until that step is done; the security group also needs inbound rules for ports 80 and 443 (and none for 8080).

**8.6 Staging Troubleshooting**

- **`ssh: connect to host ... Operation timed out`**: SSH is closed, not the server. Add the port-22 rule for your IP (8.1). The server is probably fine if `https://api.staging.opencirc.org/v3/api-docs` answers.
- **`Permission denied (publickey)`**: you are logging in as the wrong user. Use `opencirc@`.
- **`502 Bad Gateway` from the public URL**: Caddy is up but the app is not listening on 8080, usually because it is restarting (a redeploy takes about a minute) or died. Check `app-status` and `app-tail`.
- **"App process exited before becoming ready"**: the JVM died during startup. Run `app-tail` or `app-logs` for the stack trace. Usual causes are a failed Flyway migration or the database container being down (`docker ps`).
- **"App did not become ready within 180s"**: the JVM is alive but not listening yet. Check `app-tail`; on a cold JVM under memory pressure startup can exceed the timeout, in which case `TIMEOUT_SECONDS=300 bash scripts/wait-for-app.sh` re-checks without restarting.
- **`Invalid character found in method name [0x16...]` in old logs**: harmless. Internet scanners sent TLS handshakes to port 8080 while it was public; since Caddy fronts the app this no longer happens.
