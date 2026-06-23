$ErrorActionPreference = "Stop"

function Add-UserPath($pathToAdd) {
    $userPath = [Environment]::GetEnvironmentVariable("Path", "User")
    if ($userPath -notlike "*$pathToAdd*") {
        [Environment]::SetEnvironmentVariable("Path", "$userPath;$pathToAdd", "User")
    }
    if ($env:Path -notlike "*$pathToAdd*") {
        $env:Path = "$env:Path;$pathToAdd"
    }
}

$wingetCandidates = @(
    "$env:LOCALAPPDATA\Microsoft\WindowsApps\winget.exe",
    "winget.exe"
)
$winget = $wingetCandidates | Where-Object { Get-Command $_ -ErrorAction SilentlyContinue } | Select-Object -First 1

if ($winget) {
    Write-Host "winget gefunden: $winget"
    & $winget install -e --id EclipseAdoptium.Temurin.17.JDK --accept-package-agreements --accept-source-agreements
    & $winget install -e --id Git.Git --accept-package-agreements --accept-source-agreements
    & $winget install -e --id Google.AndroidStudio --accept-package-agreements --accept-source-agreements
    & $winget install -e --id Microsoft.VisualStudioCode --accept-package-agreements --accept-source-agreements
} else {
    Write-Host "winget nicht gefunden. Ich öffne die offiziellen Downloads. Installiere sie und starte danach dieses Skript erneut."
    Start-Process "https://developer.android.com/studio"
    Start-Process "https://adoptium.net/temurin/releases/?version=17&os=windows&arch=x64&package=jdk"
    Start-Process "https://git-scm.com/install/windows"
}

Write-Host "Installiere/aktualisiere Android Platform-Tools nach C:\Android\platform-tools ..."
New-Item -ItemType Directory -Force "C:\Android" | Out-Null
$zip = "$env:TEMP\platform-tools-latest-windows.zip"
Invoke-WebRequest -Uri "https://dl.google.com/android/repository/platform-tools-latest-windows.zip" -OutFile $zip
if (Test-Path "C:\Android\platform-tools") { Remove-Item "C:\Android\platform-tools" -Recurse -Force }
Expand-Archive $zip -DestinationPath "C:\Android" -Force
Add-UserPath "C:\Android\platform-tools"

$androidHome = "$env:LOCALAPPDATA\Android\Sdk"
[Environment]::SetEnvironmentVariable("ANDROID_HOME", $androidHome, "User")
[Environment]::SetEnvironmentVariable("ANDROID_SDK_ROOT", $androidHome, "User")
$env:ANDROID_HOME = $androidHome
$env:ANDROID_SDK_ROOT = $androidHome

Write-Host ""
Write-Host "Fertig. Öffne Android Studio einmal, lasse die Standard-SDK-Komponenten installieren, dann baue mit:"
Write-Host "powershell -ExecutionPolicy Bypass -File .\scripts\build-debug.ps1"
