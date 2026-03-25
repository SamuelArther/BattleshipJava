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
$distDir = Join-Path $projectRoot "dist"
$packageDir = Join-Path $projectRoot "package"
$resourcesDir = Join-Path $projectRoot "resources"
$sources = Get-ChildItem -Path (Join-Path $projectRoot "src") -Recurse -Filter *.java | ForEach-Object { $_.FullName }
$compileClasspath = "$JavaFxLib\javafx.controls.jar;$JavaFxLib\javafx.graphics.jar;$JavaFxLib\javafx.base.jar;$JavaFxLib\javafx.media.jar"
$jarPath = Join-Path $distDir "Battleship.jar"
$appImageDir = Join-Path $packageDir "Battleship"

Write-Host "Building classes..."
Remove-Item $outDir -Recurse -Force -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Force -Path $outDir | Out-Null
javac -Xlint:none -cp $compileClasspath -d $outDir $sources
Copy-Item (Join-Path $resourcesDir "*") $outDir -Recurse -Force

Write-Host "Creating jar..."
Remove-Item $distDir -Recurse -Force -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Force -Path $distDir | Out-Null
jar --create --file $jarPath -C $outDir .

Write-Host "Building Windows app-image..."
Remove-Item $packageDir -Recurse -Force -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Force -Path $packageDir | Out-Null

$appImageArgs = @(
    "--type", "app-image",
    "--name", "Battleship",
    "--dest", $packageDir,
    "--input", $distDir,
    "--main-jar", "Battleship.jar",
    "--main-class", "Main",
    "--java-options", "--enable-native-access=javafx.graphics,javafx.media --module-path `"$JavaFxLib`" --add-modules javafx.controls,javafx.media"
)

jpackage @appImageArgs
Write-Host "Done. App image created in: $appImageDir"
