#!/bin/bash
# ============================================================
# RaftKVStore - Single Node Startup Script (Singleton Mode)
# ============================================================
# In singleton mode, the node directly becomes Leader without
# going through the Raft election process. Suitable for
# development, testing, or single-node deployment.
#
# Usage:
#   ./start-singleton.sh              # Start with defaults
#   ./start-singleton.sh --build      # Force rebuild before start
#
# Environment variables:
#   NODE_ID    - Node ID (default: node-1)
#   HOST       - Listen host (default: 127.0.0.1)
#   PORT       - Listen port (default: 6666)
#   BASE_PATH  - Data storage path (default: ./data/singleton)
#   JAVA_OPTS  - Extra JVM options (default: -Xms256m -Xmx1g)
# ============================================================

set -e

# ---- Configuration ----
NODE_ID="${NODE_ID:-node-1}"
HOST="${HOST:-127.0.0.1}"
PORT="${PORT:-6666}"
BASE_PATH="${BASE_PATH:-./data/singleton}"
JAVA_OPTS="${JAVA_OPTS:--Xms256m -Xmx1g}"

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

JAR_FILE="server/target/server-1.0-jar-with-dependencies.jar"

# ---- Functions ----
build_project() {
    echo "=============================================="
    echo " Building RaftKVStore..."
    echo "=============================================="
    mvn clean package -DskipTests -pl server -am
    echo ""
}

cleanup() {
    echo ""
    echo "Stopping RaftKVStore singleton node..."
    if [ -n "$JAVA_PID" ] && kill -0 "$JAVA_PID" 2>/dev/null; then
        kill "$JAVA_PID"
        wait "$JAVA_PID" 2>/dev/null
        echo "Node stopped (PID: $JAVA_PID)"
    fi
    exit 0
}

# ---- Main ----
if [ "$1" = "--build" ]; then
    build_project
fi

if [ ! -f "$JAR_FILE" ]; then
    echo "JAR file not found. Building project..."
    build_project
fi

# Ensure data directory exists
mkdir -p "$BASE_PATH"

# Trap signals for clean shutdown
trap cleanup SIGINT SIGTERM

echo "=============================================="
echo " RaftKVStore - Singleton Mode"
echo "=============================================="
echo " Node ID:    $NODE_ID"
echo " Host:       $HOST"
echo " Port:       $PORT"
echo " Data Path:  $BASE_PATH"
echo "=============================================="
echo ""

java $JAVA_OPTS \
    -jar "$JAR_FILE" \
    -i "$NODE_ID" \
    -h "$HOST" \
    -p "$PORT" \
    -m singleton &
JAVA_PID=$!

echo "Node started (PID: $JAVA_PID)"

# Wait for the Java process
wait $JAVA_PID
