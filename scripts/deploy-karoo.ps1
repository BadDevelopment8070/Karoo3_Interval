$ErrorActionPreference = "Stop"
$ProjectRoot = Resolve-Path "$PSScriptRoot\.."
Set-Location $ProjectRoot

$apk = "$ProjectRoot\dist\smart-ftp-field-v1.1.3-debug.apk"
if (-not (Test-Path $apk)) {
    Write-Host "APK fehlt. Ich baue zuerst."
    & "$PSScriptRoot\build-debug.ps1"
}

$adbCandidates = @(
    "$env:ANDROID_HOME\platform-tools\adb.exe",
    "C:\Android\platform-tools\adb.exe",
    "adb.exe"
)
$adb = $adbCandidates | Where-Object { Get-Command $_ -ErrorAction SilentlyContinue } | Select-Object -First 1
if (-not $adb) { throw "adb nicht gefunden. Starte scripts\setup-windows-tools.ps1." }

Write-Host "ADB: $adb"
& $adb kill-server | Out-Null
& $adb start-server | Out-Null
& $adb devices
Write-Host "Wenn hier 'unauthorized' steht: Karoo entsperren und RSA-Fingerprint bestätigen."
Read-Host "Drücke ENTER zum Installieren"
& $adb install -r $apk
Write-Host "Installiert. Auf Karoo: Apps -> Smart FTP Field öffnen, Training wählen, dann Datenfeld 'Smart FTP Workout' in eine große Ride-Seite legen."
