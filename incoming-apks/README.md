# Ablage für neue APKs

Hier legst du eine frisch gebaute APK ab, wenn du willst, dass sie als Update
in Liams Appstore auftaucht. Einfach in diesem Ordner über die GitHub-Weboberfläche
hochladen (Dateiname egal — Paketname und Version werden aus der APK selbst gelesen).

Danach in Claude Code sagen: **"Schau im apk-Ordner nach, es gibt ein Update."**

Claude übernimmt den Rest: Version aus der APK lesen, GitHub-Release anlegen,
`apps.json` aktualisieren, pushen. Die Datei hier im Ordner wird danach wieder
gelöscht — der Ordner ist nur eine Zwischenablage für unverarbeitete Uploads,
kein dauerhafter Speicherort.
