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
   `GET /repos/<owner>/<repo>/releases/latest` ab (siehe
   `SelfUpdateManager.kt`). Der Workflow
   [`​.github/workflows/release.yml`](.github/workflows/release.yml) baut bei
   jedem Tag-Push (`v1`, `v2`, `v3`, …) automatisch eine neue APK und hängt
   sie als Release-Asset an. Das ist die "Update-Struktur über GitHub" für
   den Store selbst.

**Wichtig:** Die Zahl im Tag (`v7`) muss mit `versionCode` in
`app/build.gradle.kts` übereinstimmen, sonst erkennt der Selbstupdate-Check
die neue Version nicht.

## Repo-Struktur (ein Repo für alles)

```
liams-appstore/                  <- dieses Repo, öffentlich auf GitHub
├── app/                         <- Android-Studio-Projekt (dieser Ordner)
├── apps.json                    <- Katalog: welche Apps im Store erscheinen
├── .github/workflows/release.yml<- baut+released den Store bei Tag-Push
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
      "apkSha256": "",                     // optional: SHA-256 der APK, wird vor Install geprüft
      "minSdk": 26,
      "installs": 4
    }
  ]
}
```

**Fehlt ein Feld** (Screenshots, Teaser, Changelog, Reviews), zeigt die
Detailseite den jeweiligen Abschnitt einfach nicht an — kein Absturz, nur
eine schlankere Shop-Seite.

## Die zwei mitgelieferten Apps

`apps.json` enthält bereits zwei echte Apps, mit Daten direkt aus den
jeweiligen APKs (`aapt dump badging`) bzw. dem Repo ausgelesen:

- **Impulskauf-Stopper** (`com.liam.kaptalismusaufhalter`) — hat schon ein
  eigenes Repo mit eigener Update-Struktur
  ([obipepenobi-hub/impulskauf-stopper](https://github.com/obipepenobi-hub/impulskauf-stopper)).
  `apkUrl` zeigt direkt auf dessen aktuelles Release (`v1.0.3`) — die Prüfsumme
  ist bereits eingetragen, das funktioniert **sofort**, sobald `apps.json` in
  einem Repo liegt, das die App abfragt.
- **Cadence** (`com.liamkkm.cadence`) — die Android-Fernbedienung für den
  lokalen KI-DJ aus dem `spotify-dj`-Projekt. Dafür existiert noch **kein**
  GitHub-Release, `apkUrl` in `apps.json` zeigt also noch ins Leere
  (`obipepenobi-hub/liams-appstore/releases/download/cadence-v1.0.36/...`).
  Dieser Befehl macht den Eintrag scharf:
  ```bash
  gh release create cadence-v1.0.36 \
    "C:\Users\liam\OneDrive\Desktop\Android apks claude\Cadence.apk" \
    --repo obipepenobi-hub/liams-appstore \
    --title "Cadence 1.0.36"
  ```
  Die Prüfsumme in `apps.json` (`apkSha256`) passt schon zu genau dieser
  Datei — beim Hochladen nichts umbenennen oder erneut komprimieren, sonst
  stimmt der Hash nicht mehr und die Installation wird abgelehnt.

## Eine neue App-Version veröffentlichen

```bash
# im Ordner deines ANDEREN App-Projekts (z.B. Grillwetter), nach dem Build:
gh release create grillwetter-v0.10 app-release.apk \
  --repo obipepenobi-hub/liams-appstore \
  --title "Grillwetter 0.10"
```

Danach in `apps.json` (in diesem Repo) `version`, `versionCode` und `apkUrl`
der App aktualisieren und pushen. Liams Appstore zeigt beim nächsten Start
automatisch "Update" an.

## Liams Appstore selbst veröffentlichen (Self-Update)

```bash
# versionCode in app/build.gradle.kts z.B. auf 2 erhöhen, committen, dann:
git tag v2
git push origin v2
```

Die Action baut die APK und legt das Release an. In der App unter
**Mehr → Jetzt nach Store-Update suchen** wird das sofort gefunden.

## Erstes Einrichten

Ist bereits erledigt: Repo [obipepenobi-hub/liams-appstore](https://github.com/obipepenobi-hub/liams-appstore)
ist angelegt, `DEFAULT_OWNER` in `StoreConfig.kt` und alle Platzhalter in
`apps.json`/`README.md` zeigen bereits auf diesen Namen, der Code liegt
gepusht auf `main`.

Übrig bleibt nur noch, das Projekt lokal zu öffnen:

```bash
git clone https://github.com/obipepenobi-hub/liams-appstore.git
```

Danach in Android Studio öffnen (`File → Open` auf den geklonten Ordner),
Gradle synchronisieren lassen, auf dem Handy installieren (USB-Debugging
oder `./gradlew installDebug`).

## Warum "Installieren" trotzdem ein Systemfenster zeigt

Android lässt keine App außerhalb des Play Store APKs *lautlos* installieren
— das wäre ein Sicherheitsloch. Liams Appstore lädt die APK, prüft optional
die SHA-256-Summe und übergibt sie dann per `FileProvider` an den
System-Installer. Die eigene Bestätigungs-Seite (Berechtigungen, Größe,
Version) läuft *vor* diesem Systemdialog — inhaltlich deckungsgleich mit dem
Prototyp, technisch ehrlich zu dem, was Android erlaubt.

## Build-Hinweis

Dieses Projekt wurde ohne Android-SDK-Zugriff in der Entwicklungsumgebung
geschrieben — es konnte hier **nicht kompiliert** werden. Öffne es in
Android Studio (Hedgehog oder neuer) für den ersten echten Build; dort
zeigen sich eventuelle letzte Tippfehler sofort in der IDE.
