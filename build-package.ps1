param()
$ErrorActionPreference = "Stop"

$projectRoot  = Split-Path -Parent $MyInvocation.MyCommand.Path
$stagingDir   = Join-Path $projectRoot "package\iexpress-staging\source"
$sedPath      = Join-Path $projectRoot "package\iexpress-staging\Battleship.sed"
$outputExe    = Join-Path $projectRoot "package\Battleship.exe"
$iexpress     = "C:\Windows\System32\iexpress.exe"

# ---------------------------------------------------------------------------
# 1. Build Setup.exe
# ---------------------------------------------------------------------------
Write-Host "Building Setup.exe..."
& powershell -NoProfile -ExecutionPolicy Bypass -File (Join-Path $projectRoot "build-setup.ps1")
if ($LASTEXITCODE -ne 0) { throw "build-setup.ps1 failed." }

# ---------------------------------------------------------------------------
# 2. Copy Setup.exe and Battleship.exe into the staging folder
# ---------------------------------------------------------------------------
Write-Host "Copying executables to staging..."

$setupExe      = Join-Path $projectRoot "Setup.exe"
$launcherExe   = Join-Path $projectRoot "Battleship.exe"

if (!(Test-Path $setupExe))    { throw "Setup.exe not found — run build-setup.ps1 first." }
if (!(Test-Path $launcherExe)) { throw "Battleship.exe not found — run build-exe.ps1 first." }

Copy-Item $setupExe    (Join-Path $stagingDir "Setup.exe")    -Force
Copy-Item $launcherExe (Join-Path $stagingDir "Battleship.exe") -Force

# Ensure previously-missing files are present in staging

# ---------------------------------------------------------------------------
# 3. Build IExpress package
# ---------------------------------------------------------------------------
Write-Host "Building installer package..."
Remove-Item $outputExe -Force -ErrorAction SilentlyContinue

& $iexpress /N $sedPath /Q

# IExpress is async — wait for the output file (up to 60 s)
$waited = 0
while (!(Test-Path $outputExe) -and $waited -lt 60) {
    Start-Sleep -Seconds 1
    $waited++
}

if (Test-Path $outputExe) {
    $size = [math]::Round((Get-Item $outputExe).Length / 1MB, 1)
    Write-Host "Done. Installer: $outputExe ($size MB)"
} else {
    Write-Warning "IExpress finished but $outputExe was not found. Check the SED file paths."
}
