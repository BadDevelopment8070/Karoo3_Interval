$ErrorActionPreference = "Stop"
$ProjectRoot = Resolve-Path "$PSScriptRoot\.."
Set-Location $ProjectRoot
$adbCandidates = @("$env:ANDROID_HOME\platform-tools\adb.exe", "C:\Android\platform-tools\adb.exe", "adb.exe")
$adb = $adbCandidates | Where-Object { Get-Command $_ -ErrorAction SilentlyContinue } | Select-Object -First 1
if (-not $adb) { throw "adb nicht gefunden." }
New-Item -ItemType Directory -Force "$ProjectRoot\logs" | Out-Null
$out = "$ProjectRoot\logs\karoo-smart-ftp-logcat.txt"
Write-Host "Schreibe Log nach $out. Stoppen mit STRG+C."
& $adb logcat -v time | Tee-Object -FilePath $out
