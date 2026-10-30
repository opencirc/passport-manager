#!/usr/bin/env bash
set -euo pipefail

step() { printf '\n==> %s\n' "$1"; }
die()  { printf 'Error: %s\n' "$*" >&2; exit 1; }

require_cmd() {
  command -v "$1" >/dev/null 2>&1 || die "Missing required command: $1"
}

APP_DIR="${APP_DIR:-$HOME/passport-manager}"
SESSION="${SESSION:-passport-manager}"

require_cmd git
require_cmd mvn
require_cmd tmux

step "1) Ensuring working tree is clean"
cd "$APP_DIR"
[[ -d .git ]] || die "Not a git repo: $APP_DIR"
if ! git diff --quiet || ! git diff --cached --quiet; then
  git status --porcelain
  die "Working tree has uncommitted changes. Commit/stash them first."
fi

step "2) Pulling latest changes"
git checkout main
git reset --hard origin/main
git pull --ff-only origin main

step "3) Building (skipping tests, keeping memory limits)"
export MAVEN_OPTS="${MAVEN_OPTS:--Xms128m -Xmx768m -XX:MaxMetaspaceSize=256m -XX:MaxDirectMemorySize=128m -XX:+ExitOnOutOfMemoryError}"
mvn -T 1 -DskipTests clean package

step "4) Ensuring the launcher has the reverse proxy settings"
RUN_SCRIPT="$APP_DIR/run-host.sh"
[[ -x "$RUN_SCRIPT" ]] || die "Missing executable run script: $RUN_SCRIPT"

# Launchers written before the Caddy setup lack these exports; add them once, right after the
# REDIS_PORT line, so existing installations pick them up on their next redeploy.
ensure_launcher_export() {
  local variable_name="$1"
  local export_line="$2"
  if grep -qE "^export ${variable_name}=" "$RUN_SCRIPT"; then
    return 0
  fi
  grep -qE '^export REDIS_PORT=' "$RUN_SCRIPT" \
    || die "Cannot add ${variable_name}: no 'export REDIS_PORT=' line in $RUN_SCRIPT to anchor on"
  printf 'Adding %s to %s\n' "$variable_name" "$RUN_SCRIPT"
  # cp -p gives the temporary file the launcher's own mode and owner before it is rewritten, so
  # the replacement keeps the original permissions instead of the umask default plus chmod +x.
  cp -p "$RUN_SCRIPT" "$RUN_SCRIPT.tmp"
  awk -v line="$export_line" '{ print } /^export REDIS_PORT=/ { print line }' "$RUN_SCRIPT" > "$RUN_SCRIPT.tmp"
  mv "$RUN_SCRIPT.tmp" "$RUN_SCRIPT"
}

# Each line is inserted directly after the anchor, so the last one added ends up first.
ensure_launcher_export SERVER_FORWARD_HEADERS_STRATEGY \
  'export SERVER_FORWARD_HEADERS_STRATEGY="native" # trust X-Forwarded-* from Caddy on localhost'
ensure_launcher_export SERVER_ADDRESS \
  'export SERVER_ADDRESS="127.0.0.1"               # only Caddy (reverse proxy on this box) talks to the app directly'

step "5) Restarting app in tmux"
if tmux has-session -t "$SESSION" 2>/dev/null; then
  tmux kill-session -t "$SESSION"
fi

tmux new-session -d -s "$SESSION"
tmux rename-window -t "${SESSION}:0" "app" 2>/dev/null || true
tmux send-keys -t "${SESSION}:0.0" "bash -lc '$RUN_SCRIPT'" C-m

step "6) Waiting for the app to become ready"
SESSION="$SESSION" bash "$APP_DIR/scripts/wait-for-app.sh"

echo "App started in tmux session: $SESSION"
echo "To view logs: app-logs (tmux attach -t $SESSION, then Ctrl-b, d to detach) or app-tail (log file)"
