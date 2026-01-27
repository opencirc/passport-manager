#!/usr/bin/env bash
set -euo pipefail

step() { printf '\n==> %s\n' "$1"; }
die() { printf 'Error: %s\n' "$*" >&2; exit 1; }

prompt() {
  local var_name="$1"
  local message="$2"
  local default="${3:-}"
  local value=""

  if [[ -n "$default" ]]; then
    read -r -p "$message [$default]: " value
    value="${value:-$default}"
  else
    read -r -p "$message: " value
  fi

  [[ -n "$value" ]] || die "value cannot be empty"
  printf -v "$var_name" '%s' "$value"
}

require_cmd() {
  command -v "$1" >/dev/null 2>&1 || die "Missing required command: $1"
}

step "0) Sanity checks (Ubuntu 24.04 recommended)"
if [[ -r /etc/os-release ]]; then
  . /etc/os-release
  if [[ "${ID:-}" != "ubuntu" ]]; then
    die "This script is intended for Ubuntu. Detected ID=${ID:-unknown}"
  fi
fi

step "1) Base packages (git, tmux, curl, certificates)"
sudo apt-get update -y
sudo apt-get install -y ca-certificates curl git tmux gnupg wget apt-transport-https

step "2) Configure shell prompt + Maven memory cap (idempotent, persisted)"
mkdir -p "$HOME/.bashrc.d"

cat > "$HOME/.bashrc.d/opencirc.sh" <<'EOF'
# --- opencirc server prompt + Maven JVM limits ---
PS1='${debian_chroot:+($debian_chroot)}\[\033[01;32m\]\u\[\033[01;33m\]@api.staging.opencirc.org\[\033[00m\]:\[\033[01;34m\]\w\[\033[00m\]\$ '
export MAVEN_OPTS="-Xms128m -Xmx768m -XX:MaxMetaspaceSize=256m -XX:MaxDirectMemorySize=128m -XX:+ExitOnOutOfMemoryError"
# --- end ---
EOF

if ! grep -qs 'source ~/.bashrc.d/opencirc.sh' "$HOME/.bashrc"; then
  cat >> "$HOME/.bashrc" <<'EOF'

# Load OpenCirc staging helpers
if [ -f ~/.bashrc.d/opencirc.sh ]; then
  source ~/.bashrc.d/opencirc.sh
fi
EOF
fi

printf 'Note: Open a new shell (or re-login) for prompt/MAVEN_OPTS changes to apply.\n'

step "3) Install Docker Engine + Compose plugin (official Docker repo)"
sudo apt-get remove -y docker.io docker-compose docker-compose-v2 docker-doc podman-docker containerd runc || true

sudo install -m 0755 -d /etc/apt/keyrings
sudo curl -fsSL https://download.docker.com/linux/ubuntu/gpg -o /etc/apt/keyrings/docker.asc
sudo chmod a+r /etc/apt/keyrings/docker.asc

sudo tee /etc/apt/sources.list.d/docker.sources >/dev/null <<EOF
Types: deb
URIs: https://download.docker.com/linux/ubuntu
Suites: $(. /etc/os-release && echo "${UBUNTU_CODENAME:-$VERSION_CODENAME}")
Components: stable
Signed-By: /etc/apt/keyrings/docker.asc
EOF

sudo apt-get update -y
sudo apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
sudo apt-get autoremove -y

step "4) Install Java 21 (Temurin) on Ubuntu 24.04"
wget -qO - https://packages.adoptium.net/artifactory/api/gpg/key/public \
  | sudo gpg --dearmor \
  | sudo tee /etc/apt/trusted.gpg.d/adoptium.gpg >/dev/null

echo "deb https://packages.adoptium.net/artifactory/deb $(awk -F= '/^VERSION_CODENAME/{print$2}' /etc/os-release) main" \
  | sudo tee /etc/apt/sources.list.d/adoptium.list >/dev/null

sudo apt-get update -y
sudo apt-get install -y temurin-21-jdk

require_cmd java
java --version

step "5) Install Maven + PostgreSQL client"
sudo apt-get install -y maven postgresql-client
require_cmd mvn

step "6) Create an SSH key for GitHub (skips if already exists)"
mkdir -p "$HOME/.ssh"
chmod 700 "$HOME/.ssh"

KEY_PATH="$HOME/.ssh/id_ed25519"
if [[ -f "$KEY_PATH" ]]; then
  printf 'SSH key already exists at %s; skipping key generation.\n' "$KEY_PATH"
else
  prompt GIT_EMAIL "Enter the email to associate with the SSH key" "<EMAIL_ADDRESS>"
  ssh-keygen -t ed25519 -C "$GIT_EMAIL" -f "$KEY_PATH"
fi

eval "$(ssh-agent -s)" >/dev/null
ssh-add "$KEY_PATH" >/dev/null

printf '\nNow add this public key to GitHub:\n\n'
cat "${KEY_PATH}.pub"
read -r -p "Press Enter once you've added the key to GitHub to continue: " _

step "7) Configure swap (idempotent) + tuning"
if ! swapon --show | awk '{print $1}' | grep -qx '/swapfile'; then
  if [[ ! -f /swapfile ]]; then
    sudo fallocate -l 4G /swapfile
    sudo chmod 600 /swapfile
    sudo mkswap /swapfile
  fi
  sudo swapon /swapfile
fi

if ! grep -qE '^/swapfile\s' /etc/fstab; then
  echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab >/dev/null
fi

sudo tee /etc/sysctl.d/99-memory-tuning.conf >/dev/null <<'EOF'
vm.swappiness=30
vm.vfs_cache_pressure=50
EOF
sudo sysctl --system >/dev/null

step "8) Clone repo (skip if already present)"
APP_DIR="$HOME/passport-manager"

# Ensure GitHub host keys are present to avoid interactive authenticity prompt
mkdir -p "$HOME/.ssh"
chmod 700 "$HOME/.ssh"
touch "$HOME/.ssh/known_hosts"
chmod 600 "$HOME/.ssh/known_hosts"

if ! ssh-keygen -F github.com >/dev/null; then
  ssh-keyscan -H github.com >> "$HOME/.ssh/known_hosts" 2>/dev/null || true
fi

if [[ -d "$APP_DIR/.git" ]]; then
  printf 'Repo already exists at %s; skipping clone.\n' "$APP_DIR"
else
  git clone git@github.com:opencirc/passport-manager.git "$APP_DIR"
fi

cd "$APP_DIR"

step "9) Start docker services (db, cache)"
# Add user to docker group for future logins (no reliance on newgrp mid-script)
if ! id -nG "$USER" | tr ' ' '\n' | grep -qx docker; then
  sudo usermod -aG docker "$USER"
  printf 'Added %s to docker group. You may need to log out/in to run docker without sudo.\n' "$USER"
fi

# Use sudo so this works immediately in the current session
sudo docker compose up -d db cache

step "10) (Optional) Quick build under limits (guard systemd-run --user)"
if command -v systemd-run >/dev/null 2>&1; then
  if systemd-run --user --scope -p MemoryMax=1200M -p CPUQuota=100% true >/dev/null 2>&1; then
    systemd-run --user --scope -p MemoryMax=1200M -p CPUQuota=100% bash -lc 'cd "$HOME/passport-manager" && mvn -T 1 -DskipTests clean package'
  else
    printf 'systemd-run --user not available in this session; running build normally.\n'
    mvn -T 1 -DskipTests clean package
  fi
else
  printf 'systemd-run not installed; running build normally.\n'
  mvn -T 1 -DskipTests clean package
fi

step "11) Run the app in tmux (host JVM; dockerized db/cache)"
SESSION="passport-manager"
RUN_SCRIPT="$APP_DIR/run-host.sh"

cat > "$RUN_SCRIPT" <<'EOF'
#!/usr/bin/env bash
set -euo pipefail

cd "$HOME/passport-manager"

export DATABASE_URL="jdbc:postgresql://127.0.0.1:5435/opencirc"
export DATABASE_USER="postgres"
export DATABASE_PASSWORD="opencirc"
export REDIS_HOST="127.0.0.1"
export REDIS_PORT="6381"

export MAVEN_OPTS="-Xms128m -Xmx768m -XX:MaxMetaspaceSize=256m -XX:MaxDirectMemorySize=128m -XX:+ExitOnOutOfMemoryError"

echo "Starting app at $(date -Is)"

java -jar target/*.jar
EOF

chmod +x "$RUN_SCRIPT"

if tmux has-session -t "$SESSION" 2>/dev/null; then
  tmux kill-session -t "$SESSION"
fi

tmux new-session -d -s "$SESSION"
tmux rename-window -t "${SESSION}:0" "app" 2>/dev/null || true

# Kill any previously running command in that pane, then start fresh
tmux send-keys -t "${SESSION}:0.0" C-c 2>/dev/null || true
tmux send-keys -t "${SESSION}:0.0" "bash -lc '$RUN_SCRIPT'" C-m

echo "App started in tmux session: $SESSION"
echo "To view logs: tmux attach -t $SESSION  (then Ctrl-b, d to detach)"
