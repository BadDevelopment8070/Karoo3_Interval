# GitHub Token Sicherheit

Der GitHub Token ist ein Geheimnis wie ein Passwort.

## Sofortmaßnahme

Wenn ein Token in einem Chat, Screenshot, Ticket oder Repository gepostet wurde:

1. GitHub öffnen: https://github.com/settings/tokens
2. Den betroffenen Token auswählen.
3. Token löschen bzw. revoke/delete.
4. Einen neuen Token erzeugen.

## Benötigte Token für dieses Projekt

### Nur APK bauen

Für den lokalen Gradle-Build wird nur ein klassischer GitHub Personal Access Token mit folgendem Scope benötigt:

- read:packages

Dieser Token wird lokal in `%USERPROFILE%\.gradle\gradle.properties` gespeichert.
Er darf nicht in das Repository committed werden.

### Projekt zu GitHub pushen

Für Push nach GitHub nutze am besten Git Credential Manager über Git for Windows.
Falls Git nach einem Token fragt, generiere einen neuen Token lokal und gib ihn nur in Git/Windows ein.
Nicht in Chat posten.

## Dateien, die niemals committed werden sollen

- `%USERPROFILE%\.gradle\gradle.properties`
- `local.properties`
- `*.jks`
- `*.keystore`
- `dist/*.apk`
- Tokens, Passwörter, Secrets
