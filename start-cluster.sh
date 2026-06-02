#!/bin/bash
# ============================================================
# RaftKVStore - Cluster Startup Script (3-Node Cluster)
# ============================================================
# Starts a 3-node Raft cluster (A, B, C) on localhost.
# Each node runs in the background with logs written to the
# logs/ directory.
#
# Usage:
#   ./start-cluster.sh                # Start 3-node cluster
#   ./start-cluster.sh --build        # Force rebuild before start
#   ./start-cluster.sh stop           # Stop all cluster nodes
#   ./start-cluster.sh status         # Check cluster status
#
# Node configuration:
#   Node A: port 6666, connector port 6665
#   Node B: port 7777, connector port 7776
#   Node C: port 8888, connector port 8887
#
# Environment variables:
#   JAVA_OPTS  - Extra JVM options (default: -Xms256m -Xmx1g)
# ============================================================

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

JAR_FILE="server/target/server-1.0-jar-with-dependencies.jar"
LOG_DIR="./logs"
PID_DIR="$LOG_DIR"
JAVA_OPTS="${JAVA_OPTS:--Xms256m -Xmx1g}"

# Config files (relative to project root, accessible as files on disk during dev)
CONFIG_A="server/src/main/resources/A/server.properties"
CONFIG_B="server/src/main/resources/B/server.properties"
CONFIG_C="server/src/main/resources/C/server.properties"

# ---- Functions ----
build_project() {
    echo "=============================================="
    echo " Building RaftKVStore..."
    echo "=============================================="
    mvn clean package -DskipTests -pl server -am
    echo ""
}

start_node() {
    local node_label="$1"
    local config_file="$2"
    local log_file="$LOG_DIR/node-${node_label}.log"
    local pid_file="$PID_DIR/node-${node_label}.pid"

    if [ -f "$pid_file" ] && kill -0 "$(cat "$pid_file")" 2>/dev/null; then
        echo "  Node $node_label is already running (PID: $(cat "$pid_file"))"
        return 1
    fi

    echo "  Starting Node $node_label..."
    nohup java $JAVA_OPTS -jar "$JAR_FILE" -c "$config_file" \
        >> "$log_file" 2>&1 &
    local pid=$!
    echo "$pid" > "$pid_file"
    echo "  Node $node_label started (PID: $pid, log: $log_file)"
}

stop_node() {
    local node_label="$1"
    local pid_file="$PID_DIR/node-${node_label}.pid"

    if [ -f "$pid_file" ]; then
        local pid=$(cat "$pid_file")
        if kill -0 "$pid" 2>/dev/null; then
            echo "  Stopping Node $node_label (PID: $pid)..."
            kill "$pid"
            # Wait for graceful shutdown
            local waited=0
            while kill -0 "$pid" 2>/dev/null && [ $waited -lt 10 ]; do
                sleep 1
                waited=$((waited + 1))
            done
            # Force kill if still running
            if kill -0 "$pid" 2>/dev/null; then
                echo "  Force stopping Node $node_label..."
                kill -9 "$pid" 2>/dev/null || true
            fi
            echo "  Node $node_label stopped."
        else
            echo "  Node $node_label is not running (stale PID file)."
        fi
        rm -f "$pid_file"
    else
        echo "  Node $node_label is not running (no PID file)."
    fi
}

check_node_status() {
    local node_label="$1"
    local pid_file="$PID_DIR/node-${node_label}.pid"

    if [ -f "$pid_file" ]; then
        local pid=$(cat "$pid_file")
        if kill -0 "$pid" 2>/dev/null; then
            echo "  Node $node_label: RUNNING (PID: $pid)"
        else
            echo "  Node $node_label: STOPPED (stale PID: $pid)"
        fi
    else
        echo "  Node $node_label: STOPPED"
    fi
}

start_cluster() {
    echo "=============================================="
    echo " RaftKVStore - 3-Node Cluster Mode"
    echo "=============================================="

    # Create log and pid directories
    mkdir -p "$LOG_DIR" "$PID_DIR"

    # Start nodes sequentially with a brief delay for port binding
    start_node "A" "$CONFIG_A"
    sleep 2
    start_node "B" "$CONFIG_B"
    sleep 2
    start_node "C" "$CONFIG_C"

    echo ""
    echo "=============================================="
    echo " Cluster started successfully!"
    echo "=============================================="
    echo " Logs directory: $LOG_DIR"
    echo ""
    echo " Check status:  ./start-cluster.sh status"
    echo " Stop cluster:  ./start-cluster.sh stop"
    echo "=============================================="
}

stop_cluster() {
    echo "=============================================="
    echo " Stopping RaftKVStore Cluster..."
    echo "=============================================="
    stop_node "A"
    stop_node "B"
    stop_node "C"
    echo ""
    echo "Cluster stopped."
}

show_status() {
    echo "=============================================="
    echo " RaftKVStore Cluster Status"
    echo "=============================================="
    check_node_status "A"
    check_node_status "B"
    check_node_status "C"
}

cleanup() {
    echo ""
    echo "Caught interrupt signal. Stopping cluster..."
    stop_cluster
    exit 0
}

# ---- Main ----
case "$1" in
    stop)
        stop_cluster
        exit 0
        ;;
    status)
        show_status
        exit 0
        ;;
    --build)
        build_project
        ;;
esac

if [ ! -f "$JAR_FILE" ]; then
    echo "JAR file not found. Building project..."
    build_project
fi

# Verify config files exist
if [ ! -f "$CONFIG_A" ] || [ ! -f "$CONFIG_B" ] || [ ! -f "$CONFIG_C" ]; then
    echo "ERROR: Config files not found at server/src/main/resources/{A|B|C}/server.properties"
    echo "Please ensure you are running this script from the project root directory."
    exit 1
fi

# Trap signals for clean shutdown
trap cleanup SIGINT SIGTERM

start_cluster

echo ""
echo "Press Ctrl+C to stop the cluster."

# Wait indefinitely so the script can trap Ctrl+C
while true; do
    sleep 5
done
