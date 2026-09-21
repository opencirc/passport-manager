<img alt="OpenCirc" src="https://opencirc-public.s3.fr-par.scw.cloud/opencirc-logo.png" width="200" />

**Construction Materials Passport Generator and Manager**

  This application allows users to generate and manage passports for construction materials through a REST API. It uses external dictionaries such as bsDD (https://search.bsdd.buildingsmart.org/) and lexicon (https://definehub.com/en/) to generate passports in the defined templates.

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

**5. Authentication**

To use the API, login using the test admin credentials:

 **With login credentials :**

```bash
{
  "email": "admin@test.com",
  "password": "Password123!"
}
```

Once logged in, you will receive a JWT access and refresh token which must be included in requests via cookies or headers.

```bash
access_token = <received JWT access token>
refresh_token = <received JWT refresh token>
```

**With Api Keys :**
Add the below keys in the request header in the `X-Api-Key` and `X-Api-Secret` fields.
```bash
X-Api-Key: fa76a9d9-caa7-41ba-b929-f468620fa023
X-Api-Secret: API_sRynttcLE0OfRM3kFi1RXFdP_34b0c0ed5ed2
```

You can pass them either through Request header.

**6. CLI Tool Commands**

This application also provides a CLI tool to manage users, API keys, templates, and seed the database.

**6.1 Register a New User:**

```bash
register-user --email <email> --password <password> --firstName <firstName> --lastName <lastName> --role <user/admin>
```

> --role : Defaults to user

> --password : Use at least 12 characters with a mix of uppercase, lowercase, digits, or symbols.

**6.2 Create API Key Command:**

```bash
create-api-key --userId <user id> --expiration-date (optional) <Expiration date in yyyy-MM-dd  --name  name of the token
```

**6.3 List API Keys:**

```bash
list-api-tokens --user-id <user id>
```

**6.4 Data Dictionary Command:**

```bash
fetch-template --dictionaryType <<bsdd/lexicon>> --type <<class/property>> --uri <<URI of the template>> --raw <<true/false>>
```

> --raw : default is false

>true: returns the template without added fields

>false: returns processed templates with added fields


**6.5 Seeder Commands:**

Seed initial data into the database:

```bash
seed --type USER                 # Adds only user data
seed --type PASSPORT_FROM_API    # Adds passport data fetched from external API templates
seed --type PASSPORT_FROM_JSON   # Adds passport data from pre-saved bsdd_templates.json
seed --type ALL                  # Adds both user and passport data using templates from bsdd_templates.json
seed                             # Defaults to ALL
```


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

Staging runs at `api.staging.opencirc.org` on a single Ubuntu 24.04 box. The app runs as the `opencirc` user, not root: the checkout lives in `/home/opencirc/passport-manager`, the Java process runs inside a tmux session named `passport-manager`, and Postgres and Redis run in Docker (ports 5435 and 6381). If you log in as root, the home directory looks empty because nothing lives there.

**8.1 SSH Access**

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

**8.2 Shell Helpers**

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

**8.3 Deploying**

Only `main` is deployed. Merge first, then on the server:

```bash
tmux new -s deploy     # so the build survives a dropped SSH connection
app-redeploy
```

`app-redeploy` runs `scripts/redeploy-app.sh`: it refuses to run with uncommitted changes in the checkout, resets to `origin/main`, builds with `mvn -DskipTests package` under the memory limits in `MAVEN_OPTS`, restarts the `passport-manager` tmux session running `run-host.sh`, and then runs `scripts/wait-for-app.sh`. That script waits up to 180 seconds for the app to listen on port 8080 with a live Java process, and fails with a pointer to the logs otherwise. "App started" is only printed once readiness succeeds.

App output goes to both the tmux pane and `~/logs/passport-manager.log`, so logs survive restarts.

**8.4 Setting Up a Fresh Server**

`scripts/setup-staging.sh` provisions a blank Ubuntu 24.04 host end to end: packages, Docker, Temurin Java 21, Maven, swap, a GitHub deploy key, the clone, the shell helpers and tmux config, the Docker services, a first build, and the first app start. Run it as the user that should own the app (not root) and follow the prompts.

**8.5 Staging Troubleshooting**

- **`Permission denied (publickey)`**: you are logging in as the wrong user. Use `opencirc@`.
- **"App process exited before becoming ready"**: the JVM died during startup. Run `app-tail` or `app-logs` for the stack trace. Usual causes are a failed Flyway migration or the database container being down (`docker ps`).
- **"App did not become ready within 180s"**: the JVM is alive but not listening yet. Check `app-tail`; on a cold JVM under memory pressure startup can exceed the timeout, in which case `TIMEOUT_SECONDS=300 bash scripts/wait-for-app.sh` re-checks without restarting.
- **`Invalid character found in method name [0x16...]` in the logs**: harmless. Internet scanners are sending TLS handshakes to the plain HTTP port.
