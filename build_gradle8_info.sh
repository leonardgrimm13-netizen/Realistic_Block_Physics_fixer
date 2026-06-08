#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
./gradlew --info clean build
