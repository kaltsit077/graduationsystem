#!/usr/bin/env bash
set -Eeuo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

COMPOSE_FILE="docker-compose.prod.yml"
WITH_EMBEDDING=0
NO_CACHE=1

for arg in "$@"; do
  case "$arg" in
    --with-embedding)
      WITH_EMBEDDING=1
      ;;
    --no-no-cache)
      NO_CACHE=0
      ;;
    -h|--help)
      cat <<'EOF'
Usage: scripts/deploy-prod.sh [options]

Options:
  --with-embedding   Start embedding service profile
  --no-no-cache      Build images without --no-cache
  -h, --help         Show help
EOF
      exit 0
      ;;
    *)
      echo "[ERROR] Unknown option: $arg"
      exit 1
      ;;
  esac
done

if command -v docker >/dev/null 2>&1 && docker compose version >/dev/null 2>&1; then
  COMPOSE_CMD=(docker compose)
elif command -v docker-compose >/dev/null 2>&1; then
  COMPOSE_CMD=(docker-compose)
else
  echo "[ERROR] Neither 'docker compose' nor 'docker-compose' is available."
  echo "Install Docker Engine + Compose plugin first."
  exit 1
fi

echo "========================================"
echo " Prod one-click deployment"
echo "========================================"
echo "Project: $ROOT_DIR"
echo "Compose: ${COMPOSE_CMD[*]}"
echo "File:    $COMPOSE_FILE"
echo

if [[ ! -f "$COMPOSE_FILE" ]]; then
  echo "[ERROR] Missing $COMPOSE_FILE in project root."
  exit 1
fi

if [[ ! -f ".env" ]]; then
  if [[ -f ".env.example" ]]; then
    cp .env.example .env
    echo "[WARN] .env not found. Created from .env.example."
    echo "[WARN] Edit .env and replace ALL placeholder secrets before continuing:"
    echo "       MYSQL_ROOT_PASSWORD / MYSQL_PASSWORD / DB_PASSWORD / JWT_SECRET / ADMIN_PASSWORD"
  else
    echo "[ERROR] Missing .env and .env.example."
  fi
  exit 1
fi

required_vars=(
  MYSQL_ROOT_PASSWORD
  MYSQL_PASSWORD
  DB_PASSWORD
  JWT_SECRET
  ADMIN_PASSWORD
)

for key in "${required_vars[@]}"; do
  value="$(grep -E "^${key}=" .env | sed -E "s/^${key}=//" || true)"
  if [[ -z "${value// }" ]]; then
    echo "[ERROR] .env missing required variable: $key"
    exit 1
  fi
done

echo "[1/5] Checking Docker daemon..."
if ! docker info >/dev/null 2>&1; then
  echo "[ERROR] Docker daemon is not running."
  exit 1
fi
echo "[OK] Docker daemon is running."
echo

echo "[2/5] Pulling base images..."
"${COMPOSE_CMD[@]}" -f "$COMPOSE_FILE" pull || true
echo

echo "[3/5] Stopping existing services (safe, keeps volumes)..."
"${COMPOSE_CMD[@]}" -f "$COMPOSE_FILE" down
echo

echo "[4/5] Building images..."
if [[ "$NO_CACHE" -eq 1 ]]; then
  "${COMPOSE_CMD[@]}" -f "$COMPOSE_FILE" build --no-cache
else
  "${COMPOSE_CMD[@]}" -f "$COMPOSE_FILE" build
fi
echo

echo "[5/5] Starting services..."
if [[ "$WITH_EMBEDDING" -eq 1 ]]; then
  "${COMPOSE_CMD[@]}" -f "$COMPOSE_FILE" --profile embedding up -d
else
  "${COMPOSE_CMD[@]}" -f "$COMPOSE_FILE" up -d
fi
echo

echo "========================================"
echo " Deployment complete"
echo "========================================"
"${COMPOSE_CMD[@]}" -f "$COMPOSE_FILE" ps
echo
echo "Useful commands:"
echo "  Logs:  ${COMPOSE_CMD[*]} -f $COMPOSE_FILE logs -f"
echo "  Stop:  ${COMPOSE_CMD[*]} -f $COMPOSE_FILE down"
echo
