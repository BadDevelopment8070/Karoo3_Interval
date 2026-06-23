$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

$remoteUrl = "https://github.com/BadDevelopment8070/Karoo3_Interval.git"

Write-Host "Karoo Smart FTP Field - GitHub Push" -ForegroundColor Cyan
Write-Host "Target repository: $remoteUrl" -ForegroundColor Cyan
Write-Host ""
Write-Host "SECURITY: Do not paste GitHub tokens into chat or commit them into files." -ForegroundColor Yellow
Write-Host "This script does not store your GitHub token. Git/Git Credential Manager will ask you to login if needed." -ForegroundColor Yellow
Write-Host ""

if (-not (Get-Command git -ErrorAction SilentlyContinue)) {
    throw "Git wurde nicht gefunden. Bitte zuerst 1_SETUP_WINDOWS_TOOLS.bat ausführen."
}

if (-not (Test-Path ".git")) {
    git init
}

# Keep Gradle credentials and build artifacts out of Git.
$gitignore = @"
.gradle/
build/
app/build/
local.properties
*.keystore
*.jks
*.apk
dist/
*.log
"@
$gitignore | Set-Content -Path ".gitignore" -Encoding UTF8

$origin = git remote get-url origin 2>$null
if ($LASTEXITCODE -ne 0) {
    git remote add origin $remoteUrl
} elseif ($origin -ne $remoteUrl) {
    git remote set-url origin $remoteUrl
}

git add .

$hasChanges = git status --porcelain
if ([string]::IsNullOrWhiteSpace($hasChanges)) {
    Write-Host "Keine neuen Änderungen zu committen." -ForegroundColor Green
} else {
    git commit -m "Initial Karoo Smart FTP Field v1.1"
}

# Use main as branch name.
git branch -M main

Write-Host ""
Write-Host "Jetzt wird zu GitHub gepusht. Falls ein Login-Fenster erscheint: mit BadDevelopment8070 anmelden." -ForegroundColor Cyan
Write-Host "Wenn Git nach Token fragt, nutze einen NEUEN Token, nicht den im Chat geposteten." -ForegroundColor Yellow

git push -u origin main

Write-Host ""
Write-Host "Push abgeschlossen." -ForegroundColor Green
