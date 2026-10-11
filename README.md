# Liams Appstore

Ein privater Android-App-Store: zeigt deine selbstgebauten Apps (und die
deiner Freunde) aus einem GitHub-Repo an, lädt die passende APK herunter und
übergibt sie dem System-Installer — ohne dass du je wieder eine einzelne
APK-Datei manuell öffnen musst. Design 1:1 nach dem "Werkstatt"-Prototyp
(Creme/Terrakotta/Salbei, Caprasimo-Überschriften, Figtree-Fließtext,
Pillen-Buttons).

## Wie die Update-Struktur über GitHub funktioniert

Es gibt zwei getrennte Update-Mechanismen:

1. **Die gelisteten Apps** (aktuell Impulskauf-Stopper und Cadence): Liams
   Appstore lädt bei jedem Start `apps.json` aus deinem Repo (`raw.githubusercontent.com`)
   und vergleicht `versionCode` je App mit der installierten Version
   (`PackageManager`). Ist der Katalog-Wert höher, zeigt die App "Update"
   statt "Öffnen" — genau wie im Prototyp.
2. **Liams Appstore selbst**: Die App fragt periodisch
   `GET /repos/<owner>/<repo>/releases` ab und nimmt das neueste `vN`-Release
   (siehe `SelfUpdateManager.kt`). Veröffentlicht wird mit `tools/release.sh` (siehe unten): lokal bauen, mit dem
   privaten Schlüssel signieren, als Release `vN` mit Anhang
   `liams-appstore-vN.apk` hochladen. Das ist die "Update-Struktur über GitHub"
   für den Store selbst.

**Wichtig:** Die Zahl im Tag (`v7`) muss mit `versionCode` in
`app/build.gradle.kts` übereinstimmen, sonst erkennt der Selbstupdate-Check
die neue Version nicht.

## Repo-Struktur (ein Repo für alles)

```
liams-appstore/                  <- dieses Repo, öffentlich auf GitHub
├── app/                         <- Android-Studio-Projekt (dieser Ordner)
├── apps.json                    <- Katalog: welche Apps im Store erscheinen
├── .github/workflows/ci.yml     <- reine Kompilier-Prüfung (kein Release, keine Schlüssel)
├── tools/release.sh             <- baut, signiert (lokal!) und veröffentlicht den Store
├── tools/apk-info.sh            <- Paket/Version/Signatur-Hash einer APK anzeigen
├── incoming-apks/               <- Ablage für neue App-APKs (wird verarbeitet und geleert)
└── releases/                    <- GitHub Releases (kein Ordner, sondern
                                     die "Releases"-Ansicht auf github.com)
```

Jede gelistete App bekommt einen eigenen Release-Tag in genau diesem Repo,
z. B. `grillwetter-v0.9` mit Anhang `grillwetter-v0.9.apk`. `apps.json`
verweist per `apkUrl` direkt auf den `browser_download_url` dieses Assets.

## apps.json — Schema

```jsonc
{
  "storeName": "Liams Appstore",
  "friends": ["Timo", "Lena", "Jonas", "Mara"],
  "categories": [
    { "id": "werkzeuge", "name": "Werkzeuge", "subtitle": "Alltag, Haushalt, Wetter" }
  ],
  "apps": [
    {
      "id": "grillwetter",                 // eindeutig, wird für Downloads/Icons genutzt
      "packageName": "com.timo.grillwetter",// muss zur echten APK passen (für Update-Erkennung)
      "name": "Grillwetter",
      "author": "Timo",
      "category": "werkzeuge",             // muss zu einer categories[].id passen
      "version": "0.9",                    // Anzeige-Text
      "versionCode": 9,                    // Vergleichswert für "Update verfügbar"
      "sizeBytes": 4300000,
      "teaser": "Kurzer Satz für die Startseiten-Karte.",
      "description": "Längerer Absatz für 'Worum es geht'.",
      "tags": ["Kein Konto", "Widget"],
      "permissions": [
        { "label": "Standort, nur während der Nutzung", "manifestName": "ACCESS_FINE_LOCATION" }
      ],
      "changelog": [
        { "version": "0.9", "date": "29. August 2026", "notes": "Was ist neu." }
      ],
      "reviews": [
        { "author": "Lena", "initials": "LE", "quote": "Kurzes Zitat." }
      ],
      "iconUrl": "",                       // leer = Buchstaben-Platzhalter, sonst Bild-URL
      "screenshots": ["Radar", "Abend"],    // aktuell Platzhalter-Labels; echte Bild-URLs
                                            // funktionieren genauso (siehe ScreenshotThumb)
      "apkUrl": "https://github.com/<owner>/liams-appstore/releases/download/<tag>/<datei>.apk",
      "apkSha256": "<64 Hex-Zeichen>",      // PFLICHT: SHA-256 der APK, ohne sie lehnt der Store die Installation ab
      "minSdk": 26,
      "installs": 4
    }
  ]
}
```

**Fehlt ein Feld** (Screenshots, Teaser, Changelog, Reviews), zeigt die
Detailseite den jeweiligen Abschnitt einfach nicht an — kein Absturz, nur
eine schlankere Shop-Seite.

## Sicherheitsmodell

Der Store kann Apps installieren — wer ihn kapert, kapert das Handy. Deshalb:

- **Privater Signaturschlüssel, nie im Repo.** Liegt in
  `C:\Users\liam\liams-appstore-signing\` (Schlüssel + Passwort + Lineage).
  **Extern sichern** (z. B. USB/Passwortmanager): geht er verloren, kann sich die
  installierte App nie wieder aktualisieren (nur Deinstallieren + Neuinstallieren).
  Der frühere Debug-Schlüssel (`liams-appstore-debug.keystore`, Passwort `android`) war
  öffentlich und gilt als kompromittiert; per Android-Schlüsselrotation (Lineage, ab
  Android 13) wurde er abgelöst. Er steckt noch in der Git-Historie — ohne Bedeutung,
  solange v15+ installiert ist.
- **Release-APK ist nicht debuggable**, `allowBackup=false`, Tapjacking-Schutz
  (`filterTouchesWhenObscured`), GitHub-Token nur verschlüsselt (Android Keystore).
- **Jede heruntergeladene APK wird geprüft**, bevor der System-Installer sie sieht:
  SHA-256 aus dem Katalog (Pflicht), Paketname, Versionscode und Signatur-Pinning
  (`security/TrustedSigners.kt`; Store-Updates: nur das eingebaute Release-Zertifikat).
  Die Vertrauensanker liegen im Code, nicht in `apps.json` — ein manipulierter Katalog
  kann also keine fremde App einschleusen.
- **Katalog ist unvertrauenswürdige Eingabe:** ungültige `id`/`packageName` und alle
  URLs außerhalb des eigenen GitHub-Kontos werden verworfen; Netzwerkverkehr nur per
  HTTPS zu GitHub-Hosts (auch jeder Redirect-Hop), Größenlimits, sichere Dateinamen.
- **Store-Selfupdate** beachtet nur Releases mit Tag `vN` (kein Prerelease/Entwurf)
  und Anhang `liams-appstore-vN.apk`.

Empfohlen auf GitHub-Seite (nicht per Code lösbar): Zwei-Faktor/Passkey, Branch-Schutz
für `main`, Tokens mit minimalen Rechten.

## Eine neue Version einer gelisteten App veröffentlichen

1. APK in `incoming-apks/` auf `main` ablegen (oder direkt per Claude).
2. `bash tools/apk-info.sh pfad/zur.apk` — zeigt Paketname, Version, Größe, SHA-256 und
   ob der Signer im Store hinterlegt ist. **Zeigt es „NICHT hinterlegt“, nicht
   veröffentlichen.**
3. Release im Repo anlegen — **als Prerelease** (`prerelease: true`), z. B. Tag
   `cadence-v1.1.17`. Nur so bleibt `releases/latest` beim Store und App-Releases
   werden nie als Store-Update missverstanden.
4. In `apps.json` `version`, `versionCode`, `sizeBytes`, `apkUrl`, `apkSha256`
   aktualisieren, pushen, Datei aus `incoming-apks/` löschen. Der Store zeigt das
   Update innerhalb einer Minute an (Katalog wird im Vordergrund minütlich neu geladen).

### Eine komplett neue App aufnehmen

Wegen des Signatur-Pinnings braucht das eine neue Store-Version: Signer-Hash mit
`tools/apk-info.sh` ermitteln, in `security/TrustedSigners.kt` eintragen, App in
`apps.json` ergänzen, dann Store-Release (unten). Ohne Eintrag lehnt der Store die
Installation ab („Signatur dieser App ist im Store nicht hinterlegt“).

## Liams Appstore selbst veröffentlichen (Self-Update)

```bash
# 1. versionCode/versionName in app/build.gradle.kts erhöhen
# 2. committen und nach main pushen
# 3. lokal bauen, signieren, verifizieren und das Release anlegen:
bash tools/release.sh
```

Das Skript verweigert den Dienst bei ungepushten Änderungen, prüft Signatur (Lineage,
ab Android 13 neues Zertifikat), Paketname/Version und dass die APK nicht debuggable
ist, und legt erst dann das Release `vN` an. Auf dem Handy erscheint das Popup beim
nächsten Öffnen bzw. spätestens nach 5 Minuten im Vordergrund; sofort unter
**Mehr → Jetzt nach Store-Update suchen**.

## Warum „Installieren“ trotzdem ein Systemfenster zeigt

Android lässt keine App außerhalb des Play Store APKs *lautlos* installieren — das wäre
ein Sicherheitsloch. Liams Appstore lädt die APK, prüft sie (siehe Sicherheitsmodell) und
übergibt sie per `FileProvider` an den System-Installer. Die Play-Protect-Warnung
(„könnte schädlich sein“) ist bei Apps außerhalb des Play Store normal.

## Bauen

Android Studio (Hedgehog oder neuer) oder `./gradlew :app:assembleDebug` (JDK 17,
Android SDK 34). Lokale Debug-Builds sind mit dem Standard-Debug-Schlüssel signiert und
nur zum Entwickeln gedacht — verteilt wird ausschließlich über `tools/release.sh`.
