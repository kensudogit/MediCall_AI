#!/bin/sh
set -e

BACKEND_PORT="${BACKEND_PORT:-8081}"
export BACKEND_PORT
export SERVER_PORT="$BACKEND_PORT"
export API_URL="http://127.0.0.1:${BACKEND_PORT}"

resolve_database_env() {
  if [ -n "$SPRING_DATASOURCE_URL" ]; then
    echo "[start] Using SPRING_DATASOURCE_URL"
    return 0
  fi

  DB_RAW="${DATABASE_URL:-${DATABASE_PRIVATE_URL:-}}"
  if [ -n "$DB_RAW" ]; then
    echo "[start] Resolving JDBC URL from DATABASE_URL"
    eval "$(DB_RAW="$DB_RAW" node -e "
      const raw = process.env.DB_RAW;
      const u = new URL(raw.replace(/^postgres(ql)?:/,'http:'));
      const db = u.pathname.replace(/^\//,'') || 'railway';
      const host = u.hostname;
      const port = u.port || '5432';
      const user = decodeURIComponent(u.username || '');
      const pass = decodeURIComponent(u.password || '');
      console.log('export SPRING_DATASOURCE_URL=' + JSON.stringify('jdbc:postgresql://' + host + ':' + port + '/' + db));
      console.log('export SPRING_DATASOURCE_USERNAME=' + JSON.stringify(user));
      console.log('export SPRING_DATASOURCE_PASSWORD=' + JSON.stringify(pass));
    ")"
    return 0
  fi

  if [ -n "$PGHOST" ]; then
    echo "[start] Building JDBC URL from PGHOST"
    export SPRING_DATASOURCE_URL="jdbc:postgresql://${PGHOST}:${PGPORT:-5432}/${PGDATABASE:-railway}"
    export SPRING_DATASOURCE_USERNAME="${PGUSER:-postgres}"
    export SPRING_DATASOURCE_PASSWORD="${PGPASSWORD:-}"
    return 0
  fi

  echo "[start] ERROR: Database not configured."
  echo "[start] On Railway: add Postgres and set DATABASE_URL=\${{Postgres.DATABASE_URL}}"
  exit 1
}

resolve_database_env
DB_HOST="$(echo "$SPRING_DATASOURCE_URL" | sed -n 's|jdbc:postgresql://\([^:/]*\).*|\1|p')"
echo "[start] Database host: ${DB_HOST:-unknown}"

cd /app/backend
echo "[start] Launching Spring Boot on port ${BACKEND_PORT}..."
java -jar app.jar 2>&1 | tee /tmp/backend.log &
BACKEND_PID=$!

echo "[start] Waiting for backend /api/health ..."
READY=0
i=0
while [ "$i" -lt 120 ]; do
  if curl -sf "http://127.0.0.1:${BACKEND_PORT}/api/health" >/dev/null 2>&1; then
    READY=1
    echo "[start] Backend is ready"
    break
  fi
  i=$((i + 1))
  sleep 2
done

if [ "$READY" -eq 0 ]; then
  echo "[start] Backend failed to become healthy. Last log lines:"
  tail -n 40 /tmp/backend.log 2>/dev/null || true
  exit 1
fi

FRONTEND_PORT="${PORT:-3000}"
export API_URL="http://127.0.0.1:${BACKEND_PORT}"
cd /app/frontend
echo "[start] Launching Next.js on port ${FRONTEND_PORT} (backend on ${BACKEND_PORT})..."
exec npm start -- -p "${FRONTEND_PORT}" -H 0.0.0.0
