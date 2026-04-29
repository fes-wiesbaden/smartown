#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
COMPOSE_FILE="$SCRIPT_DIR/compose.yaml"
SERVICES=(mqtt backend mariadb frontend)

usage() {
  cat <<'EOF'
Usage:
  ./monitor-docker-logs.sh
  ./monitor-docker-logs.sh --dry-run

Opens one terminal tab per Docker Compose service and streams logs with timestamps.
EOF
}

require_command() {
  local cmd="$1"
  if ! command -v "$cmd" >/dev/null 2>&1; then
    printf 'Error: required command not found: %s\n' "$cmd" >&2
    exit 1
  fi
}

check_prerequisites() {
  if [[ ! -f "$COMPOSE_FILE" ]]; then
    printf 'Error: compose file not found: %s\n' "$COMPOSE_FILE" >&2
    exit 1
  fi

  require_command docker
  docker compose version >/dev/null

  for service in "${SERVICES[@]}"; do
    if ! docker compose -f "$COMPOSE_FILE" ps --services | grep -Fxq "$service"; then
      printf 'Error: service missing in compose file: %s\n' "$service" >&2
      exit 1
    fi
  done
}

build_log_command() {
  local service="$1"
  printf "cd %q && printf '\\n[%s] Live logs with Docker timestamps. Ctrl+C stops follow mode.\\n\\n' && docker compose -f %q logs -f -t --tail 100 %q; printf '\\n[%s] Log stream ended. Shell stays open.\\n'; exec bash" \
    "$SCRIPT_DIR" "$service" "$COMPOSE_FILE" "$service" "$service"
}

open_with_gnome_terminal() {
  local service

  for service in "${SERVICES[@]}"; do
    gnome-terminal \
      --title="$service" \
      --working-directory="$SCRIPT_DIR" \
      -- bash -lc "$(build_log_command "$service")" &
  done

  wait
}

open_with_x_terminal_emulator() {
  local service

  for service in "${SERVICES[@]}"; do
    x-terminal-emulator -T "$service" -e bash -lc "$(build_log_command "$service")" &
  done

  wait
}

run() {
  check_prerequisites

  if [[ "${1:-}" == "--dry-run" ]]; then
    local service
    printf 'Compose file: %s\n' "$COMPOSE_FILE"
    for service in "${SERVICES[@]}"; do
      printf '[%s] %s\n' "$service" "$(build_log_command "$service")"
    done
    return 0
  fi

  if command -v gnome-terminal >/dev/null 2>&1; then
    open_with_gnome_terminal
    return 0
  fi

  if command -v x-terminal-emulator >/dev/null 2>&1; then
    open_with_x_terminal_emulator
    return 0
  fi

  printf 'Error: no supported terminal emulator found. Install gnome-terminal or x-terminal-emulator.\n' >&2
  exit 1
}

case "${1:-}" in
  ""|--dry-run)
    run "${1:-}"
    ;;
  -h|--help)
    usage
    ;;
  *)
    usage >&2
    exit 1
    ;;
esac
