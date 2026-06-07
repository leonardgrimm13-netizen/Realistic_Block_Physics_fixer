#!/usr/bin/env bash
set -eo pipefail

REQUIRED_GRADLE="8.8"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

if [[ ! -s "$HOME/.sdkman/bin/sdkman-init.sh" ]]; then
  echo "SDKMAN wurde nicht gefunden. Installiere Gradle 8.8 manuell oder installiere SDKMAN."
  echo "Danach: sdk install gradle 8.8 && ./build_gradle8.sh"
  exit 1
fi

# shellcheck disable=SC1091
source "$HOME/.sdkman/bin/sdkman-init.sh"

if [[ ! -x "$HOME/.sdkman/candidates/gradle/${REQUIRED_GRADLE}/bin/gradle" ]]; then
  echo "Gradle ${REQUIRED_GRADLE} ist nicht installiert. Installiere es über SDKMAN ..."
  sdk install gradle "$REQUIRED_GRADLE"
fi

"$HOME/.sdkman/candidates/gradle/${REQUIRED_GRADLE}/bin/gradle" --no-daemon clean build "$@"
