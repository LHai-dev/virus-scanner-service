#!/bin/bash
# wait-for-it.sh

# This script waits for a specific host and port to become available.
# Usage: ./wait-for-it.sh <host>:<port> [--timeout=<timeout>] [--command=<command>]

TIMEOUT=30
HOST=""
PORT=""
CMD=""

# Parse arguments
while [[ $# -gt 0 ]]; do
    case "$1" in
        --timeout=*)
            TIMEOUT="${1#*=}"
            shift
            ;;
        --command=*)
            CMD="${1#*=}"
            shift
            ;;
        *)
            if [ -z "$HOST" ]; then
                HOST="$1"
            elif [ -z "$PORT" ]; then
                PORT="$1"
            fi
            shift
            ;;
    esac
done

if [ -z "$HOST" ] || [ -z "$PORT" ]; then
    echo "Error: Both host and port must be provided."
    exit 1
fi

echo "Waiting for $HOST:$PORT to be available..."

# Wait for the host and port to become available
timeout $TIMEOUT bash -c "until nc -z -v -w30 $HOST $PORT; do echo waiting for $HOST:$PORT; sleep 1; done"

if [ $? -eq 0 ]; then
    echo "$HOST:$PORT is available!"

    # If a command was provided, run it
    if [ -n "$CMD" ]; then
        exec $CMD
    fi
else
    echo "Error: Timeout waiting for $HOST:$PORT"
    exit 1
fi
