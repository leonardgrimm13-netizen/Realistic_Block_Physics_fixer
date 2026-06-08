#!/usr/bin/env bash
set -euo pipefail
APP_HOME="$(cd "$(dirname "$0")" && pwd -P)"
PROPS="$APP_HOME/gradle/wrapper/gradle-wrapper.properties"
DIST_URL="https://services.gradle.org/distributions/gradle-8.8-bin.zip"
if [[ -f "$PROPS" ]]; then
  DIST_URL="$(sed -n 's/^distributionUrl=//p' "$PROPS" | sed 's#\\:#:#g')"
fi
if command -v gradle >/dev/null 2>&1; then
  VERSION="$(gradle --version | awk '/^Gradle / {print $2; exit}')"
  case "$VERSION" in
    8.*) exec gradle --no-daemon "$@" ;;
  esac
fi
CACHE_DIR="${GRADLE_USER_HOME:-$HOME/.gradle}/wrapper/rbpf-gradle-8.8"
GRADLE_BIN="$CACHE_DIR/gradle-8.8/bin/gradle"
if [[ ! -x "$GRADLE_BIN" ]]; then
  mkdir -p "$CACHE_DIR"
  ZIP="$CACHE_DIR/gradle-8.8-bin.zip"
  echo "Gradle 8.x not found; downloading Gradle 8.8 from $DIST_URL" >&2
  if command -v curl >/dev/null 2>&1; then
    curl -fL "$DIST_URL" -o "$ZIP"
  elif command -v wget >/dev/null 2>&1; then
    wget -O "$ZIP" "$DIST_URL"
  else
    echo "Neither curl nor wget is available to download Gradle 8.8." >&2
    exit 1
  fi
  (cd "$CACHE_DIR" && unzip -q "$ZIP")
fi
exec "$GRADLE_BIN" --no-daemon "$@"
