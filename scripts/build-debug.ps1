$ErrorActionPreference = "Stop"
$ProjectRoot = Resolve-Path "$PSScriptRoot\.."
Set-Location $ProjectRoot

$LogDir = Join-Path $ProjectRoot "logs"
New-Item -ItemType Directory -Force $LogDir | Out-Null
$LogFile = Join-Path $LogDir "latest-build.log"
if (Test-Path $LogFile) { Remove-Item $LogFile -Force }

function Log($message) {
    $line = "[{0}] {1}" -f (Get-Date -Format "yyyy-MM-dd HH:mm:ss"), $message
    Write-Host $line
    Add-Content -Encoding UTF8 -Path $LogFile -Value $line
}

function Quote-Argument([string]$arg) {
    if ($null -eq $arg) { return '""' }
    if ($arg -notmatch '[\s"]') { return $arg }
    return '"' + ($arg -replace '"', '\"') + '"'
}

function Run-Logged([string]$exe, [string[]]$arguments, [int]$TimeoutSec = 0, [switch]$AllowFailure) {
    if ($null -eq $arguments) { $arguments = @() }
    $argLine = (($arguments | ForEach-Object { Quote-Argument $_ }) -join ' ')
    Log "RUN: $exe $argLine"

    $outFile = Join-Path $LogDir "process-out.tmp"
    $errFile = Join-Path $LogDir "process-err.tmp"
    if (Test-Path $outFile) { Remove-Item $outFile -Force }
    if (Test-Path $errFile) { Remove-Item $errFile -Force }

    $process = Start-Process -FilePath $exe -ArgumentList $argLine -WorkingDirectory $ProjectRoot -NoNewWindow -PassThru -RedirectStandardOutput $outFile -RedirectStandardError $errFile
    $started = Get-Date
    while (-not $process.HasExited) {
        Start-Sleep -Seconds 5
        if ($TimeoutSec -gt 0 -and ((Get-Date) - $started).TotalSeconds -gt $TimeoutSec) {
            try { $process.Kill() } catch {}
            Log "TIMEOUT nach $TimeoutSec Sekunden: $exe $argLine"
            if (Test-Path $outFile) { Get-Content $outFile -Raw | Add-Content -Encoding UTF8 -Path $LogFile }
            if (Test-Path $errFile) { Get-Content $errFile -Raw | Add-Content -Encoding UTF8 -Path $LogFile }
            throw "Build hing länger als $TimeoutSec Sekunden. Details in $LogFile"
        }
    }
    $process.WaitForExit()

    if (Test-Path $outFile) {
        $out = Get-Content $outFile -Raw
        if ($out) { Write-Host $out; Add-Content -Encoding UTF8 -Path $LogFile -Value $out }
    }
    if (Test-Path $errFile) {
        $err = Get-Content $errFile -Raw
        if ($err) { Write-Host $err; Add-Content -Encoding UTF8 -Path $LogFile -Value $err }
    }
    Log "EXITCODE: $($process.ExitCode)"

    if ($process.ExitCode -ne 0 -and -not $AllowFailure) {
        throw "Befehl fehlgeschlagen: $exe $argLine -- Details in $LogFile"
    }
    return $process.ExitCode
}

function Add-ProcessPath($pathToAdd) {
    if (Test-Path $pathToAdd) {
        if ($env:Path -notlike "*$pathToAdd*") { $env:Path = "$env:Path;$pathToAdd" }
    }
}

function Find-JavaHome {
    if ($env:JAVA_HOME -and (Test-Path "$env:JAVA_HOME\bin\java.exe")) { return $env:JAVA_HOME }
    $roots = @("C:\Program Files\Eclipse Adoptium", "C:\Program Files\Java", "C:\Program Files\Android\Android Studio\jbr")
    foreach ($r in $roots) {
        if (Test-Path $r) {
            if (Test-Path "$r\bin\java.exe") { return $r }
            $jdk = Get-ChildItem $r -Directory -ErrorAction SilentlyContinue |
                Where-Object { $_.Name -match "17|jdk-17|temurin" } |
                Sort-Object Name -Descending |
                Select-Object -First 1
            if ($jdk -and (Test-Path "$($jdk.FullName)\bin\java.exe")) { return $jdk.FullName }
        }
    }
    return $null
}

function Find-SdkManager($androidHome) {
    $candidates = @(
        "$androidHome\cmdline-tools\latest\bin\sdkmanager.bat",
        "$androidHome\cmdline-tools\bin\sdkmanager.bat",
        "$androidHome\tools\bin\sdkmanager.bat"
    )
    foreach ($c in $candidates) { if (Test-Path $c) { return $c } }
    if (Test-Path "$androidHome\cmdline-tools") {
        $found = Get-ChildItem "$androidHome\cmdline-tools" -Recurse -Filter sdkmanager.bat -ErrorAction SilentlyContinue | Select-Object -First 1
        if ($found) { return $found.FullName }
    }
    return $null
}

function Install-CmdlineTools($androidHome) {
    $sdkManager = Find-SdkManager $androidHome
    if ($sdkManager) { return $sdkManager }

    Log "sdkmanager fehlt. Lade Android Command-line Tools direkt von Google."
    $toolsRoot = Join-Path $androidHome "cmdline-tools"
    $latestRoot = Join-Path $toolsRoot "latest"
    New-Item -ItemType Directory -Force $toolsRoot | Out-Null
    if (Test-Path $latestRoot) { Remove-Item $latestRoot -Recurse -Force }

    $zip = Join-Path $env:TEMP "commandlinetools-win-latest.zip"
    Invoke-WebRequest -Uri "https://dl.google.com/android/repository/commandlinetools-win-14742923_latest.zip" -OutFile $zip
    $tmp = Join-Path $env:TEMP "karoo_cmdline_tools"
    if (Test-Path $tmp) { Remove-Item $tmp -Recurse -Force }
    Expand-Archive $zip -DestinationPath $tmp -Force
    Move-Item (Join-Path $tmp "cmdline-tools") $latestRoot
    Remove-Item $tmp -Recurse -Force

    $sdkManager = Find-SdkManager $androidHome
    if (-not $sdkManager) { throw "sdkmanager konnte nach Command-line-Tools-Installation nicht gefunden werden." }
    return $sdkManager
}

Log "Karoo Smart FTP Field v1.1.3 Build startet"
Log "ProjectRoot=$ProjectRoot"

$javaHome = Find-JavaHome
if (-not $javaHome) { throw "JDK 17 nicht gefunden. Starte 1_SETUP_WINDOWS_TOOLS.bat oder installiere Eclipse Temurin JDK 17. Log: $LogFile" }
$env:JAVA_HOME = $javaHome
Add-ProcessPath "$javaHome\bin"
Log "JAVA_HOME=$env:JAVA_HOME"
Run-Logged "java" @("-version") 60 -AllowFailure | Out-Null

$androidHome = $env:ANDROID_HOME
if (-not $androidHome) { $androidHome = "$env:LOCALAPPDATA\Android\Sdk" }
New-Item -ItemType Directory -Force $androidHome | Out-Null
$env:ANDROID_HOME = $androidHome
$env:ANDROID_SDK_ROOT = $androidHome
Add-ProcessPath "$androidHome\platform-tools"
Add-ProcessPath "$androidHome\cmdline-tools\latest\bin"
Log "ANDROID_HOME=$env:ANDROID_HOME"

$sdkManager = Install-CmdlineTools $androidHome
Log "sdkmanager=$sdkManager"

$licenseInput = (("y" + [Environment]::NewLine) * 160)
$licenseInput | & $sdkManager --sdk_root=$androidHome --licenses 2>&1 | Tee-Object -FilePath $LogFile -Append | Out-Host
if ($LASTEXITCODE -ne 0) { Log "Warnung: sdkmanager --licenses exit=$LASTEXITCODE; Build wird trotzdem versucht." }

$packages = @("platform-tools", "platforms;android-35", "build-tools;35.0.0", "cmdline-tools;latest")
foreach ($pkg in $packages) {
    Log "SDK Package sicherstellen: $pkg"
    & $sdkManager --sdk_root=$androidHome $pkg 2>&1 | Tee-Object -FilePath $LogFile -Append | Out-Host
    if ($LASTEXITCODE -ne 0) { throw "sdkmanager konnte Paket $pkg nicht installieren. Details in $LogFile" }
}

$sdkEscaped = $androidHome.Replace("\", "\\")
"sdk.dir=$sdkEscaped" | Set-Content -Encoding ASCII "$ProjectRoot\local.properties"
Log "local.properties geschrieben"

$gradlePropsDir = "$env:USERPROFILE\.gradle"
$gradleProps = "$gradlePropsDir\gradle.properties"
New-Item -ItemType Directory -Force $gradlePropsDir | Out-Null
$propsText = if (Test-Path $gradleProps) { Get-Content $gradleProps -Raw } else { "" }
if ($propsText -notmatch "gpr\.user" -or $propsText -notmatch "gpr\.key") {
    Write-Host "karoo-ext liegt in GitHub Packages. Dafür braucht Gradle einmalig einen GitHub Personal Access Token classic mit read:packages."
    $user = Read-Host "GitHub Username"
    $token = Read-Host "GitHub Token mit read:packages"
    @"

gpr.user=$user
gpr.key=$token
"@ | Add-Content -Encoding ASCII $gradleProps
    Log "Gradle GitHub-Package-Credentials in $gradleProps ergänzt."
} else { Log "Gradle GitHub-Package-Credentials vorhanden." }

function Get-GradleExe {
    $cmd = Get-Command gradle.bat -ErrorAction SilentlyContinue
    if ($cmd) { return $cmd.Source }
    $version = "8.10.2"
    $base = "$env:USERPROFILE\.gradle\manual"
    $gradleDir = "$base\gradle-$version"
    $exe = "$gradleDir\bin\gradle.bat"
    if (-not (Test-Path $exe)) {
        New-Item -ItemType Directory -Force $base | Out-Null
        $zip = "$env:TEMP\gradle-$version-bin.zip"
        Log "Gradle nicht gefunden. Lade Gradle $version herunter ..."
        Invoke-WebRequest -Uri "https://services.gradle.org/distributions/gradle-$version-bin.zip" -OutFile $zip
        Expand-Archive $zip -DestinationPath $base -Force
    }
    return $exe
}

$gradleExe = Get-GradleExe
Log "Gradle=$gradleExe"
Run-Logged $gradleExe @("--no-daemon", "--console=plain", "clean", ":app:assembleDebug", "--stacktrace") 2700

$apks = Get-ChildItem -Path "$ProjectRoot\app\build\outputs\apk" -Filter "*.apk" -Recurse -ErrorAction SilentlyContinue | Sort-Object LastWriteTime -Descending
if (-not $apks -or $apks.Count -eq 0) { throw "Gradle meldete Erfolg, aber keine APK wurde gefunden. Details in $LogFile" }
$apk = $apks[0].FullName
New-Item -ItemType Directory -Force "$ProjectRoot\dist" | Out-Null
$dest = "$ProjectRoot\dist\smart-ftp-field-v1.1.3-debug.apk"
Copy-Item $apk $dest -Force
Log "APK gefunden: $apk"
Log "APK kopiert: $dest"
Write-Host ""
Write-Host "FERTIG: $dest"
Write-Host "Build-Log: $LogFile"
