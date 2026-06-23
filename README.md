# Karoo Smart FTP Field v1.1.3

Komplettes Android/Kotlin-Projekt für ein Hammerhead Karoo 3 Datenfeld.

## Feature-Umfang v1.1.3

- Grafisches Karoo-Datenfeld `Smart FTP Workout`
- Großes 80s-Future-Cockpit-Layout mit Power-Graph oben
- Workout-Bibliothek mit FTP-bildenden Einheiten unter ca. 1h
- Immer 10 min Warm-up und 10 min Cool-down
- K3 / Low-Cadence-Intervalle inklusive Kadenzziel
- FTP aus Karoo UserProfile, optional manueller Fallback/Override
- Work-Intervalle zählen nur herunter, wenn 3s-Power mindestens `Ziel - 5% FTP` erreicht
- Warm-up, Recovery und Cool-down laufen normal nach Zeit
- In-Ride-Alerts und Beep-Patterns bei Segmentwechseln
- Bonus Actions: Reset und nächstes Segment
- Lokale Mini-Historie abgeschlossener Trainings
- Windows-Build- und Deploy-Skripte

## Schnellstart Windows 11

1. ZIP entpacken, z. B. nach:

```text
C:\dev\karoo-smart-ftp-field-v1_1
```

2. Doppelklick:

```text
1_SETUP_WINDOWS_TOOLS.bat
```

3. Android Studio einmal öffnen und das Standard-SDK installieren lassen.

4. Doppelklick:

```text
2_BUILD_APK.bat
```

Beim ersten Build fragt das Skript nach deinem GitHub Username und einem GitHub Personal Access Token classic mit Scope `read:packages`, weil `karoo-ext` über GitHub Packages geladen wird.

5. Karoo vorbereiten:

```text
Settings -> About -> Build Number 7x tippen
Settings -> Developer Options -> USB Debugging einschalten
```

6. Karoo per USB-C anschließen, RSA-Fingerprint bestätigen, dann Doppelklick:

```text
3_DEPLOY_TO_KAROO.bat
```

7. Auf dem Karoo:

```text
Apps -> Smart FTP Field öffnen
Training auswählen
Training starten / Reset drücken
Ride Profile -> Datenfeld Smart FTP Workout auf eine große 1-Feld-Seite legen
```

## Wichtiges Bedienmodell

Die App ist bewusst als Karoo-Datenfeld gebaut. Die eigentliche Fahrt wird nicht pausiert. Nur der Work-Intervalltimer wird angehalten, wenn die Zielleistung nicht erreicht wird.

Beispiel bei FTP 280 W und Ziel 80%:

- Ziel: 224 W
- Gate: 210 W
- Work-Countdown läuft ab 210 W oder mehr
- unter 210 W: Work-Countdown steht
- Recovery läuft unabhängig von Leistung weiter

## Enthaltene Workouts

- FTP Builder 4x5
- Sweet Spot 3x10
- FTP Builder 5x4
- Over/Under 3x9
- VO2 Einstieg 6x3
- Threshold 3x8
- Microburst 2x10
- Tempo -> FTP 4x6
- K3 Low Cadence 5x4
- K3 Torque 4x6
- K3 Strength 3x8
- K3 Torque Ladder
- Sustained 2x15
- FTP Pyramid
- Traffic Safe 3x5 @80

## Wichtige Dateien

```text
app/src/main/kotlin/com/bastiankahuna/karoosmartftp/MainActivity.kt
app/src/main/kotlin/com/bastiankahuna/karoosmartftp/karoo/KarooSmartFtpExtension.kt
app/src/main/kotlin/com/bastiankahuna/karoosmartftp/karoo/SmartFtpDataType.kt
app/src/main/kotlin/com/bastiankahuna/karoosmartftp/model/WorkoutLibrary.kt
app/src/main/kotlin/com/bastiankahuna/karoosmartftp/engine/WorkoutEngine.kt
app/src/main/kotlin/com/bastiankahuna/karoosmartftp/ui/FieldRenderer.kt
```

## Build-Ausgabe

Nach erfolgreichem Build liegt die APK hier:

```text
dist\smart-ftp-field-v1.1.3-debug.apk
```

## Falls etwas nicht geht

Logs sammeln:

```text
powershell -ExecutionPolicy Bypass -File .\scripts\collect-logs.ps1
```

Danach `logs\karoo-smart-ftp-logcat.txt` anschauen oder weitergeben.

## Einschränkung

Ich kann in der ChatGPT-Sandbox kein reales Karoo-Gerät anschließen und ohne deinen GitHub-Package-Token kein signiertes APK aus `karoo-ext` bauen. Das Projekt ist deshalb so geliefert, dass dein Windows-Rechner die fehlenden externen Teile automatisiert lädt und dann das APK erzeugt.


## GitHub Repository

Dein Ziel-Repository ist bereits voreingestellt:

```text
https://github.com/BadDevelopment8070/Karoo3_Interval.git
```

Zum Hochladen des vollständigen Projekts nach GitHub:

```text
4_PUSH_TO_GITHUB.bat
```

Wichtig: GitHub Tokens niemals in Chats posten und niemals committen. Siehe `docs/SECURITY_TOKEN.md`.


## v1.1.3 Build-Fix

Diese Version installiert/prüft Android SDK Platform 35 automatisch, fängt Gradle-Fehler korrekt ab und schreibt eine vollständige Logdatei nach `logs\latest-build.log`. Das APK wird nicht mehr über einen festen Pfad angenommen, sondern nach dem Build automatisch gesucht und nach `dist\smart-ftp-field-v1.1.3-debug.apk` kopiert.
