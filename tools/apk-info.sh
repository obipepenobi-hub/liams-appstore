#!/usr/bin/env bash
# Zeigt Paketname, Version, Prüfsumme und Signatur-Hash einer APK - und ob der Signer im
# Store hinterlegt ist (security/TrustedSigners.kt). Pflicht-Schritt, bevor eine APK aus
# incoming-apks/ im Katalog veröffentlicht wird.
#   bash tools/apk-info.sh pfad/zur/app.apk
set -euo pipefail
cd "$(dirname "$0")/.."
APK="${1:?APK-Pfad fehlt}"
BT="${ANDROID_HOME:-/f/android-sdk}/build-tools/34.0.0"
export JAVA_HOME="${JAVA_HOME:-/c/Program Files/Eclipse Adoptium/jdk-17.0.19.10-hotspot}"
export PATH="$JAVA_HOME/bin:$PATH"
WAPK="$(cygpath -m "$APK")"

"$BT/aapt.exe" dump badging "$WAPK" | grep -E "^package:" | sed "s/ platformBuild.*//"
echo "sizeBytes: $(stat -c%s "$APK")"
echo "sha256:    $(sha256sum "$APK" | cut -d' ' -f1)"
"$BT/apksigner.bat" verify -v --print-certs "$WAPK" | grep -E "^Verifies|Number of signers|certificate DN|certificate SHA-256"

PKG=$(sed -n "s/^package: name='\([^']*\)'.*/\1/p" <("$BT/aapt.exe" dump badging "$WAPK"))
SIGNERS=$("$BT/apksigner.bat" verify --print-certs "$WAPK" | sed -n 's/.*certificate SHA-256 digest: //p' | tr 'A-F' 'a-f')
for s in $SIGNERS; do
  if grep -q "$s" app/src/main/java/com/liam/appstore/security/TrustedSigners.kt app/build.gradle.kts; then
    echo "-> Signer $s ist im Store hinterlegt."
  else
    echo "-> ACHTUNG: Signer $s ist NICHT im Store hinterlegt (TrustedSigners.kt) - der Store lehnt diese APK ab."
  fi
done
echo "Paket: $PKG"
