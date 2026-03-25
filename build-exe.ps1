param()

$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $projectRoot

$sourcePath = Join-Path $projectRoot "Launcher.cs"
$targetPath = Join-Path $projectRoot "Battleship.exe"
$frameworkCsc = "C:\Windows\Microsoft.NET\Framework64\v4.0.30319\csc.exe"

if (!(Test-Path $sourcePath)) {
    throw "Launcher source not found: $sourcePath"
}

if (!(Test-Path $frameworkCsc)) {
    throw "C# compiler not found: $frameworkCsc"
}

Write-Host "Building Battleship.exe..."
Remove-Item $targetPath -Force -ErrorAction SilentlyContinue

& $frameworkCsc `
    /nologo `
    /target:winexe `
    /out:$targetPath `
    /reference:System.dll `
    /reference:System.Windows.Forms.dll `
    $sourcePath

if ($LASTEXITCODE -ne 0) {
    throw "Failed to compile Battleship.exe"
}

Write-Host "Done. Exe created at: $targetPath"
