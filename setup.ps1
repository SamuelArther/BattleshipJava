# Battleship Setup Wizard
# Checks for and installs all required components, then launches the game.
# Run as Administrator for automatic installs.

param(
    [switch]$Silent
)

$ErrorActionPreference = "Stop"

function Write-Step($msg) { Write-Host "`n==> $msg" -ForegroundColor Cyan }
function Write-OK($msg)   { Write-Host "    [OK] $msg" -ForegroundColor Green }
function Write-Warn($msg) { Write-Host "    [!]  $msg" -ForegroundColor Yellow }
function Write-Fail($msg) { Write-Host "    [X]  $msg" -ForegroundColor Red }

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path

# ---------------------------------------------------------------------------
# 1. Check Java (need 17+)
# ---------------------------------------------------------------------------
Write-Step "Checking Java..."

$javaOk = $false
$javaPath = $null

try {
    $javaVersion = & java -version 2>&1 | Select-String "version" | Select-Object -First 1
    if ($javaVersion -match '"(\d+)') {
        $major = [int]$Matches[1]
        if ($major -ge 17) {
            Write-OK "Java $major found."
            $javaOk = $true
            $javaPath = (Get-Command java).Source
        } else {
            Write-Warn "Java $major is too old (need 17+)."
        }
    }
} catch {
    Write-Warn "Java not found in PATH."
}

if (-not $javaOk) {
    Write-Step "Attempting to install Java 21 via winget..."
    try {
        winget install --id Microsoft.OpenJDK.21 --accept-package-agreements --accept-source-agreements -e | Out-Host
        # Refresh PATH
        $env:Path = [System.Environment]::GetEnvironmentVariable("Path","Machine") + ";" + [System.Environment]::GetEnvironmentVariable("Path","User")
        $javaVersion = & java -version 2>&1 | Select-String "version" | Select-Object -First 1
        Write-OK "Java installed: $javaVersion"
        $javaOk = $true
    } catch {
        Write-Fail "Could not install Java automatically."
        Write-Host "    Please install Java 17+ from https://adoptium.net and re-run this script." -ForegroundColor Yellow
        if (-not $Silent) { Read-Host "Press Enter to exit" }
        exit 1
    }
}

# ---------------------------------------------------------------------------
# 2. Locate or download JavaFX SDK
# ---------------------------------------------------------------------------
Write-Step "Checking JavaFX SDK..."

$javafxLib = $null

# Common locations to check
$candidates = @(
    "C:\Users\samue\Downloads\openjfx-26_windows-x64_bin-sdk\javafx-sdk-26\lib",
    "$scriptDir\javafx-sdk-lib",
    "$env:LOCALAPPDATA\javafx\lib",
    "$env:ProgramFiles\javafx\lib"
)

foreach ($path in $candidates) {
    if (Test-Path "$path\javafx.controls.jar") {
        $javafxLib = $path
        Write-OK "JavaFX found: $javafxLib"
        break
    }
}

if (-not $javafxLib) {
    Write-Warn "JavaFX SDK not found. Downloading JavaFX 21 LTS..."
    $downloadDir = "$env:LOCALAPPDATA\javafx"
    $zipPath     = "$downloadDir\openjfx.zip"
    $sdkRoot     = "$downloadDir\sdk"

    New-Item -ItemType Directory -Force -Path $downloadDir | Out-Null

    $url = "https://download2.gluonhq.com/openjfx/21.0.3/openjfx-21.0.3_windows-x64_bin-sdk.zip"
    Write-Host "    Downloading from: $url" -ForegroundColor Gray
    try {
        Invoke-WebRequest -Uri $url -OutFile $zipPath -UseBasicParsing
        Expand-Archive -Path $zipPath -DestinationPath $sdkRoot -Force
        Remove-Item $zipPath -ErrorAction SilentlyContinue
        $javafxLib = (Get-ChildItem -Path $sdkRoot -Recurse -Filter "javafx.controls.jar" | Select-Object -First 1).DirectoryName
        if ($javafxLib) {
            Write-OK "JavaFX downloaded and extracted to: $javafxLib"
        } else {
            throw "Extracted archive but could not locate javafx.controls.jar"
        }
    } catch {
        Write-Fail "Failed to download/extract JavaFX: $_"
        Write-Host "    Manually download from https://gluonhq.com/products/javafx/ (Windows SDK)" -ForegroundColor Yellow
        Write-Host "    Extract it and re-run this script." -ForegroundColor Yellow
        if (-not $Silent) { Read-Host "Press Enter to exit" }
        exit 1
    }
}

# ---------------------------------------------------------------------------
# 3. Build the project
# ---------------------------------------------------------------------------
Write-Step "Building Battleship..."

Set-Location $scriptDir

$outDir       = Join-Path $scriptDir "out"
$resourcesDir = Join-Path $scriptDir "resources"
$sources      = Get-ChildItem -Path (Join-Path $scriptDir "src") -Recurse -Filter *.java | ForEach-Object { $_.FullName }
$cp           = "$javafxLib\javafx.controls.jar;$javafxLib\javafx.graphics.jar;$javafxLib\javafx.base.jar;$javafxLib\javafx.media.jar"

if (!(Test-Path $outDir)) {
    New-Item -ItemType Directory -Force -Path $outDir | Out-Null
}

$mainClass   = Join-Path $outDir "Main.class"
$needsBuild  = !(Test-Path $mainClass)

if (!$needsBuild) {
    $latestInput  = ($sources + (Get-ChildItem $resourcesDir -Recurse -File).FullName | Get-Item | Sort-Object LastWriteTime -Descending | Select-Object -First 1).LastWriteTime
    $latestOutput = (Get-ChildItem $outDir -Recurse -File | Sort-Object LastWriteTime -Descending | Select-Object -First 1).LastWriteTime
    $needsBuild   = $latestInput -gt $latestOutput
}

if ($needsBuild) {
    Remove-Item $outDir -Recurse -Force -ErrorAction SilentlyContinue
    New-Item -ItemType Directory -Force -Path $outDir | Out-Null
    & java -jar "$javafxLib\..\bin\javac" -Xlint:none -cp $cp -d $outDir $sources 2>&1
    # Fallback to plain javac
    if ($LASTEXITCODE -ne 0) {
        & javac -Xlint:none -cp $cp -d $outDir $sources
    }
    Copy-Item (Join-Path $resourcesDir "*") $outDir -Recurse -Force
    Write-OK "Build complete."
} else {
    Write-OK "Already up-to-date, skipping recompile."
}

# ---------------------------------------------------------------------------
# 4. Write/update run.ps1 with discovered javafxLib path
# ---------------------------------------------------------------------------
Write-Step "Updating run.ps1 with detected JavaFX path..."

$runScript = @"
param(
    [string]`$JavaFxLib = "$javafxLib"
)
`$ErrorActionPreference = "Stop"
if (!(Test-Path `$JavaFxLib)) { throw "JavaFX lib folder not found: `$JavaFxLib" }
`$projectRoot = Split-Path -Parent `$MyInvocation.MyCommand.Path
Set-Location `$projectRoot
`$outDir = Join-Path `$projectRoot "out"
`$resourcesDir = Join-Path `$projectRoot "resources"
`$sources = Get-ChildItem -Path (Join-Path `$projectRoot "src") -Recurse -Filter *.java | ForEach-Object { `$_.FullName }
`$resourceFiles = Get-ChildItem -Path `$resourcesDir -Recurse -File
`$compileClasspath = "`$JavaFxLib\javafx.controls.jar;`$JavaFxLib\javafx.graphics.jar;`$JavaFxLib\javafx.base.jar;`$JavaFxLib\javafx.media.jar"
if (!(Test-Path `$outDir)) { New-Item -ItemType Directory -Force -Path `$outDir | Out-Null }
`$mainClass = Join-Path `$outDir "Main.class"
`$needsBuild = !(Test-Path `$mainClass)
if (!`$needsBuild) {
    `$latestInput = (`$sources + `$resourceFiles.FullName | Get-Item | Sort-Object LastWriteTime -Descending | Select-Object -First 1).LastWriteTime
    `$latestOutput = (Get-ChildItem -Path `$outDir -Recurse -File | Sort-Object LastWriteTime -Descending | Select-Object -First 1).LastWriteTime
    `$needsBuild = `$latestInput -gt `$latestOutput
}
if (`$needsBuild) {
    Write-Host "Building Battleship..."
    Remove-Item `$outDir -Recurse -Force -ErrorAction SilentlyContinue
    New-Item -ItemType Directory -Force -Path `$outDir | Out-Null
    javac -Xlint:none -cp `$compileClasspath -d `$outDir `$sources
    Copy-Item (Join-Path `$resourcesDir "*") `$outDir -Recurse -Force
}
Write-Host "Launching Battleship..."
java --enable-native-access=javafx.graphics,javafx.media --module-path `$JavaFxLib --add-modules javafx.controls,javafx.media -cp `$outDir Main
"@

Set-Content -Path (Join-Path $scriptDir "run.ps1") -Value $runScript -Encoding UTF8
Write-OK "run.ps1 updated."

# ---------------------------------------------------------------------------
# 5. Launch
# ---------------------------------------------------------------------------
Write-Step "Launching Battleship..."
& java --enable-native-access=javafx.graphics,javafx.media --module-path $javafxLib --add-modules javafx.controls,javafx.media -cp $outDir Main
