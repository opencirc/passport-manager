#!/usr/bin/env bash
# Waits until the app in the tmux session is accepting connections on its HTTP port.
# Exits non-zero if the tmux session disappears, the app pane returns to a prompt, or the timeout elapses.
set -euo pipefail

SESSION="${SESSION:-passport-manager}"
APP_PORT="${APP_PORT:-8080}"
TIMEOUT_SECONDS="${TIMEOUT_SECONDS:-180}"
POLL_INTERVAL_SECONDS=3

die() { printf 'Error: %s\n' "$*" >&2; exit 1; }

app_process_is_running() {
  # The pane runs run-host.sh (bash), and java is one of its descendants. Walk the tree looking for it.
  local pane_pid frontier
  pane_pid="$(tmux display-message -p -t "${SESSION}:0.0" '#{pane_pid}' 2>/dev/null)" || return 1
  frontier="$pane_pid"
  while [[ -n "$frontier" ]]; do
    if ps -o comm= -p "${frontier// /,}" | grep -qx java; then
      return 0
    fi
    frontier="$(pgrep -P "${frontier// /,}" | tr '\n' ' ' | sed 's/ $//')"
  done
  return 1
}

app_is_listening() {
  (exec 3<>"/dev/tcp/127.0.0.1/${APP_PORT}") 2>/dev/null
}

printf 'Waiting up to %ss for the app to listen on port %s...\n' "$TIMEOUT_SECONDS" "$APP_PORT"
deadline=$(( $(date +%s) + TIMEOUT_SECONDS ))

while (( $(date +%s) < deadline )); do
  # Require our own java process too, so a stale process holding the port is not mistaken for readiness.
  if app_is_listening && app_process_is_running; then
    printf 'App is ready on port %s.\n' "$APP_PORT"
    exit 0
  fi
  # Sleep first so a freshly launched run-host.sh has time to spawn java before we check on it.
  sleep "$POLL_INTERVAL_SECONDS"
  app_process_is_running || die "App process exited before becoming ready. Inspect with: tmux attach -t $SESSION (or app-logs / app-tail)"
done

die "App did not become ready within ${TIMEOUT_SECONDS}s. Inspect with: tmux attach -t $SESSION (or app-logs / app-tail)"
