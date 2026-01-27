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

step "4) Restarting app in tmux"
RUN_SCRIPT="$APP_DIR/run-host.sh"
[[ -x "$RUN_SCRIPT" ]] || die "Missing executable run script: $RUN_SCRIPT"

if tmux has-session -t "$SESSION" 2>/dev/null; then
  tmux kill-session -t "$SESSION"
fi

tmux new-session -d -s "$SESSION"
tmux rename-window -t "${SESSION}:0" "app" 2>/dev/null || true
tmux send-keys -t "${SESSION}:0.0" "bash -lc '$RUN_SCRIPT'" C-m

echo "App started in tmux session: $SESSION"
echo "To view logs: tmux attach -t $SESSION  (then Ctrl-b, d to detach)"
