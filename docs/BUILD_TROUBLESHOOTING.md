# Build Troubleshooting

## Wichtig ab v1.1.3

`2_BUILD_APK.bat` schreibt eine vollständige Logdatei nach:

```text
logs\latest-build.log
```

Wenn der Build scheitert, ist diese Datei der einzige relevante Fehlerbericht. Die letzten 30 Zeilen reichen oft nicht, weil Gradle den echten Fehler oberhalb des Blocks `Try: Run with --stacktrace` ausgibt.

## Automatisch erledigt

Das Build-Skript versucht automatisch:

- JDK 17 zu finden
- Android SDK zu finden
- `platform-tools` zu installieren
- `platforms;android-35` zu installieren
- `build-tools;35.0.0` zu installieren
- Android-SDK-Lizenzen anzunehmen
- Gradle herunterzuladen, falls kein Gradle installiert ist
- GitHub-Package-Credentials aus `%USERPROFILE%\.gradle\gradle.properties` zu verwenden
- jede gefundene APK automatisch nach `dist\smart-ftp-field-v1.1.3-debug.apk` zu kopieren

## Token

Der GitHub Token darf nie in Chat, E-Mail oder GitHub committed werden. Er gehört nur lokal in:

```text
%USERPROFILE%\.gradle\gradle.properties
```

Benötigter Scope für den Build:

```text
read:packages
```
