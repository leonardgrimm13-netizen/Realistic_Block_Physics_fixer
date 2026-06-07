#!/usr/bin/env bash
set -eo pipefail

# ForgeGradle 6.x does not support Gradle 9 yet. This project is pinned to Gradle 8.x.
REQUIRED_GRADLE="8.8"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

run_build() {
  local gradle_bin="$1"
  shift || true
  "$gradle_bin" --no-daemon clean build "$@"
}

version_major() {
  "$1" --version 2>/dev/null | awk '/^Gradle / { split($2, a, "."); print a[1]; exit }'
}

# Prefer the Gradle Wrapper if the user later generates/copies one into the project.
if [[ -x ./gradlew ]]; then
  major="$(version_major ./gradlew || true)"
  if [[ "$major" == "9" ]]; then
    echo "FEHLER: Dieser Gradle Wrapper nutzt Gradle 9.x. ForgeGradle 6.x unterstützt Gradle 9 noch nicht."
    echo "Nutze ./build_gradle8.sh oder stelle den Wrapper auf Gradle ${REQUIRED_GRADLE}."
    exit 1
  fi
  run_build ./gradlew "$@"
  exit 0
fi

# User has SDKMAN. Use/install a local Gradle 8.x without changing the system default permanently.
if [[ -s "$HOME/.sdkman/bin/sdkman-init.sh" ]]; then
  # shellcheck disable=SC1091
  source "$HOME/.sdkman/bin/sdkman-init.sh"
  if [[ ! -x "$HOME/.sdkman/candidates/gradle/${REQUIRED_GRADLE}/bin/gradle" ]]; then
    echo "Gradle ${REQUIRED_GRADLE} ist nicht installiert. Installiere es über SDKMAN ..."
    sdk install gradle "$REQUIRED_GRADLE"
  fi
  run_build "$HOME/.sdkman/candidates/gradle/${REQUIRED_GRADLE}/bin/gradle" "$@"
  exit 0
fi

if command -v gradle >/dev/null 2>&1; then
  major="$(version_major gradle || true)"
  if [[ "$major" == "9" ]]; then
    echo "FEHLER: Du nutzt Gradle 9.x, aber ForgeGradle 6.x unterstützt Gradle 9 noch nicht."
    echo "Installiere Gradle ${REQUIRED_GRADLE}, z. B.:"
    echo "  sdk install gradle ${REQUIRED_GRADLE}"
    echo "  sdk use gradle ${REQUIRED_GRADLE}"
    echo "  gradle --no-daemon clean build"
    exit 1
  fi
  run_build gradle "$@"
  exit 0
fi

echo "Kein Gradle gefunden. Empfohlen: SDKMAN installieren und dann ./build.sh erneut ausführen."
exit 1
