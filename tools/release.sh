#!/usr/bin/env bash
# Baut, signiert und veröffentlicht eine neue Store-Version - komplett lokal.
#
# Warum lokal statt GitHub Actions: Der private Signaturschlüssel darf das Gerät nicht
# verlassen. Läge er als GitHub-Secret vor, könnte jeder mit Schreibzugriff auf das Repo
# (gestohlenes Token, gekapertes Konto) per Workflow-Änderung daran kommen und gültige
# Updates für dein Handy signieren.
#
# Ablauf:  1) Version in app/build.gradle.kts erhöhen  2) committen + pushen
#          3) bash tools/release.sh
set -euo pipefail
cd "$(dirname "$0")/.."

KEYDIR="${LIAMS_SIGNING_DIR:-$HOME/liams-appstore-signing}"
SDK="${ANDROID_HOME:-/f/android-sdk}"
BT="$SDK/build-tools/34.0.0"
export ANDROID_HOME="$SDK"
export JAVA_HOME="${JAVA_HOME:-/c/Program Files/Eclipse Adoptium/jdk-17.0.19.10-hotspot}"
export PATH="$JAVA_HOME/bin:$PATH"
OWNER_REPO="obipepenobi-hub/liams-appstore"
win() { cygpath -m "$1"; }

for f in "$KEYDIR/liams-appstore-release.p12" "$KEYDIR/keystore-password.txt" "$KEYDIR/old-debug-K0.keystore" "$KEYDIR/store-cert-sha256.txt"; do
  [ -f "$f" ] || { echo "Fehlt: $f"; exit 1; }
done

CODE=$(sed -n 's/.*versionCode = \([0-9]*\).*/\1/p' app/build.gradle.kts | head -1)
NAME=$(sed -n 's/.*versionName = "\([^"]*\)".*/\1/p' app/build.gradle.kts | head -1)
TAG="v$CODE"
echo "== Release $TAG (versionName $NAME) =="

git diff --quiet && git diff --cached --quiet || { echo "Es gibt nicht committete Änderungen - erst committen und pushen."; exit 1; }
git fetch -q origin main
[ "$(git rev-parse HEAD)" = "$(git rev-parse origin/main)" ] || { echo "HEAD ist nicht gepusht/aktuell mit origin/main."; exit 1; }

OUT="$(mktemp -d)"
./gradlew :app:assembleRelease --no-daemon -q
UNSIGNED="app/build/outputs/apk/release/app-release-unsigned.apk"
[ -f "$UNSIGNED" ] || { echo "Unsigned-APK nicht gefunden: $UNSIGNED"; exit 1; }

"$BT/zipalign.exe" -f -p 4 "$(win "$UNSIGNED")" "$(win "$OUT/aligned.apk")"

# Lineage: Beweis, dass der alte Schlüssel (K0) den neuen (K1) autorisiert -> Update ohne Neuinstallation.
if [ ! -f "$KEYDIR/lineage.bin" ]; then
  "$BT/apksigner.bat" rotate --out "$(win "$KEYDIR/lineage.bin")" \
    --old-signer --ks "$(win "$KEYDIR/old-debug-K0.keystore")" --ks-pass pass:android --ks-key-alias androiddebugkey \
    --new-signer --ks "$(win "$KEYDIR/liams-appstore-release.p12")" --ks-pass "file:$(win "$KEYDIR/keystore-password.txt")" --ks-key-alias liams-appstore-release
fi

SIGNED="$OUT/liams-appstore-$TAG.apk"
"$BT/apksigner.bat" sign \
  --ks "$(win "$KEYDIR/old-debug-K0.keystore")" --ks-pass pass:android --ks-key-alias androiddebugkey \
  --next-signer \
  --ks "$(win "$KEYDIR/liams-appstore-release.p12")" --ks-pass "file:$(win "$KEYDIR/keystore-password.txt")" --ks-key-alias liams-appstore-release \
  --lineage "$(win "$KEYDIR/lineage.bin")" \
  --out "$(win "$SIGNED")" "$(win "$OUT/aligned.apk")"

echo "== Verifikation =="
"$BT/apksigner.bat" verify -v --print-certs "$(win "$SIGNED")" | grep -E "^Verifies|scheme|signers|certificate SHA-256" || true
EXPECTED=$(cat "$KEYDIR/store-cert-sha256.txt")
CERTS=$("$BT/apksigner.bat" verify --min-sdk-version 33 --print-certs "$(win "$SIGNED")")
grep -qi "$EXPECTED" <<< "$CERTS" \
  || { echo "ABBRUCH: Signer ab Android 13 ist nicht das erwartete Release-Zertifikat."; exit 1; }
"$BT/aapt.exe" dump badging "$(win "$SIGNED")" | grep -q "package: name='com.liam.appstore' versionCode='$CODE'" \
  || { echo "ABBRUCH: Paketname/versionCode in der APK passt nicht zu $TAG."; exit 1; }
if "$BT/aapt.exe" dump xmltree "$(win "$SIGNED")" AndroidManifest.xml | grep -q "android:debuggable"; then
  echo "ABBRUCH: APK ist debuggable."; exit 1
fi

echo "== GitHub-Release anlegen =="
TOKEN=$(git-credential-manager.exe get <<< $'protocol=https\nhost=github.com\n' 2>/dev/null | grep '^password=' | cut -d= -f2-)
[ -n "$TOKEN" ] || { echo "Kein GitHub-Token vom Credential Manager."; exit 1; }
python3 - "$TAG" "$NAME" > "$OUT/release.json" <<'PY'
import json, sys
tag, name = sys.argv[1], sys.argv[2]
print(json.dumps({"tag_name": tag, "target_commitish": "main", "name": "Liams Appstore " + tag,
                  "body": "Store-Version " + name + ". Installation ueber den Store-Selfupdate; signiert lokal mit dem privaten Release-Schluessel.",
                  "draft": False, "prerelease": False, "make_latest": "true"}))
PY
RESP="$OUT/resp.json"
curl -sS -X POST -H "Authorization: Bearer $TOKEN" -H "Accept: application/vnd.github+json" \
  "https://api.github.com/repos/$OWNER_REPO/releases" -d @"$OUT/release.json" -o "$RESP"
RELEASE_ID=$(python3 -c "import json,sys; print(json.load(open(sys.argv[1])).get('id',''))" "$RESP")
[ -n "$RELEASE_ID" ] || { echo "Release-Anlage fehlgeschlagen:"; cat "$RESP"; exit 1; }
curl -sS -X POST -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/vnd.android.package-archive" \
  --data-binary @"$SIGNED" \
  "https://uploads.github.com/repos/$OWNER_REPO/releases/$RELEASE_ID/assets?name=liams-appstore-$TAG.apk" -o "$OUT/asset.json"
python3 -c "import json,sys; d=json.load(open(sys.argv[1])); print(d.get('name'), d.get('size'), d.get('browser_download_url'))" "$OUT/asset.json"
TOKEN=""
sha256sum "$SIGNED"
echo "Fertig: https://github.com/$OWNER_REPO/releases/tag/$TAG"
