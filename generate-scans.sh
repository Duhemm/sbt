#!/usr/bin/env sh

DV_HOST="dv-helm-cluster-unstable-main.grdev.net"
LOGS_DIR="generate-scans-logs"
CACHE_DIR="local-cache"

get_logs() {
  LOG_FILE="$LOGS_DIR/run-$(ls "$LOGS_DIR" | wc -l).txt"

  echo \$ "$@"

  > "$LOG_FILE" >&1 "$@"

  grep "$DV_HOST" "$LOG_FILE"
}

rm -rf "$LOGS_DIR"
mkdir -p "$LOGS_DIR"
rm -rf "$CACHE_DIR"

echo "Run without any caching..."
get_logs \
  sbt -Ddevelocity.cache.remote.enabled=false -Ddevelocity.cache.local.enabled=false "-Ddevelocity.cache.local.directory=$CACHE_DIR" clean compile
echo

echo "Populating build cache..."
get_logs \
  sbt -Ddevelocity.cache.remote.enabled=false "-Ddevelocity.cache.local.directory=$CACHE_DIR" clean compile
echo

echo "Re-using build cache..."
get_logs \
  sbt -Ddevelocity.cache.remote.enabled=false "-Ddevelocity.cache.local.directory=$CACHE_DIR" clean compile
echo


