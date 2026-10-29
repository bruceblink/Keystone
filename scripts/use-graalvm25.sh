#!/usr/bin/env bash
set -euo pipefail
source /workspace/.tools/activate-graalvm25.sh
exec "$@"
