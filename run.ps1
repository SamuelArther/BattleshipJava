param(
    [string]$JavaFxLib = "C:\Users\samue\Downloads\openjfx-26_windows-x64_bin-sdk\javafx-sdk-26\lib"
)

$ErrorActionPreference = "Stop"

if (!(Test-Path $JavaFxLib)) {
    throw "JavaFX lib folder not found: $JavaFxLib"
}

$projectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $projectRoot

$outDir = Join-Path $projectRoot "out"
$resourcesDir = Join-Path $projectRoot "resources"
$sources = Get-ChildItem -Path (Join-Path $projectRoot "src") -Recurse -Filter *.java | ForEach-Object { $_.FullName }
$resourceFiles = Get-ChildItem -Path $resourcesDir -Recurse -File
$compileClasspath = "$JavaFxLib\javafx.controls.jar;$JavaFxLib\javafx.graphics.jar;$JavaFxLib\javafx.base.jar;$JavaFxLib\javafx.media.jar"

if (!(Test-Path $outDir)) {
    New-Item -ItemType Directory -Force -Path $outDir | Out-Null
}

$mainClass = Join-Path $outDir "Main.class"
$needsBuild = !(Test-Path $mainClass)

if (!$needsBuild) {
    $latestInput = ($sources + $resourceFiles.FullName | Get-Item | Sort-Object LastWriteTime -Descending | Select-Object -First 1).LastWriteTime
    $latestOutput = (Get-ChildItem -Path $outDir -Recurse -File | Sort-Object LastWriteTime -Descending | Select-Object -First 1).LastWriteTime
    $needsBuild = $latestInput -gt $latestOutput
}

if ($needsBuild) {
    Write-Host "Building Battleship..."
    Remove-Item $outDir -Recurse -Force -ErrorAction SilentlyContinue
    New-Item -ItemType Directory -Force -Path $outDir | Out-Null
    javac -Xlint:none -cp $compileClasspath -d $outDir $sources
    Copy-Item (Join-Path $resourcesDir "*") $outDir -Recurse -Force
}

Write-Host "Launching Battleship..."
java --enable-native-access=javafx.graphics,javafx.media --module-path $JavaFxLib --add-modules javafx.controls,javafx.media -cp $outDir Main
