# shellcheck shell=bash
# --- opencirc staging shell helpers (sourced from ~/.bashrc; installed to ~/.bashrc.d/opencirc.sh by setup-staging.sh) ---

# Colored prompt regardless of $TERM (stock Ubuntu only colors it for TERM=xterm-color).
# debian_chroot is set by the stock ~/.bashrc before this file is sourced.
# shellcheck disable=SC2154
PS1='${debian_chroot:+($debian_chroot)}\[\033[01;32m\]\u\[\033[01;33m\]@\h\[\033[00m\]:\[\033[01;34m\]\w\[\033[00m\]\$ '

export MAVEN_OPTS="-Xms128m -Xmx768m -XX:MaxMetaspaceSize=256m -XX:MaxDirectMemorySize=128m -XX:+ExitOnOutOfMemoryError"

export PASSPORT_MANAGER_APP_DIR="$HOME/passport-manager"
export PASSPORT_MANAGER_TMUX_SESSION="passport-manager"
export PASSPORT_MANAGER_LOG_FILE="$HOME/logs/passport-manager.log"

alias app-sessions='tmux ls'
alias app-logs='tmux attach -t "$PASSPORT_MANAGER_TMUX_SESSION"'
alias app-tail='tail -n 200 -f "$PASSPORT_MANAGER_LOG_FILE"'
alias app-redeploy='bash "$PASSPORT_MANAGER_APP_DIR/scripts/redeploy-app.sh"'
alias app-cd='cd "$PASSPORT_MANAGER_APP_DIR"'

app-status() {
  printf '\n== git ==\n'
  git -C "$PASSPORT_MANAGER_APP_DIR" log --oneline -1
  printf '\n== java process ==\n'
  pgrep -af 'java -jar' || printf '(not running)\n'
  printf '\n== tmux sessions ==\n'
  tmux ls 2>/dev/null || printf '(none)\n'
  printf '\n== docker ==\n'
  docker ps --format 'table {{.Names}}\t{{.Status}}\t{{.Ports}}' 2>/dev/null || sudo docker ps --format 'table {{.Names}}\t{{.Status}}\t{{.Ports}}'
  printf '\n== log file ==\n'
  ls -lh "$PASSPORT_MANAGER_LOG_FILE" 2>/dev/null || printf '(no log file yet, app was started before file logging was added)\n'
  printf '\nHelpers: app-sessions app-logs app-tail app-status app-redeploy app-cd\n'
}

printf 'Staging helpers loaded. Type app-status for an overview.\n'
# --- end ---
