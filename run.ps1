# BattleshipJava launcher
$ErrorActionPreference = 'Stop'
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$outDir    = Join-Path $scriptDir "out"

function Find-JavaFX {
    # Valid SDK: has javafx.controls.jar AND DLLs in same dir OR sibling bin/
    function Has-Dlls($p) {
        if (Get-ChildItem $p -Filter "prism_d3d.dll" -EA SilentlyContinue | Select-Object -First 1) { return $true }
        $bin = Join-Path (Split-Path $p -Parent) "bin"
        if (Get-ChildItem $bin -Filter "prism_d3d.dll" -EA SilentlyContinue | Select-Object -First 1) { return $true }
        return $false
    }
    function Valid-SDK($p) {
        (Test-Path (Join-Path $p "javafx.controls.jar")) -and (Has-Dlls $p)
    }

    $locs = @(
        "$env:LOCALAPPDATA\javafx\lib",
        "$env:ProgramFiles\javafx\lib"
    )
    foreach ($p in $locs) { if (Valid-SDK $p) { return $p } }

    foreach ($root in @("$env:LOCALAPPDATA\javafx", "$env:USERPROFILE\Downloads")) {
        $hit = Get-ChildItem $root -Recurse -Filter "javafx.controls.jar" -EA SilentlyContinue |
               Where-Object { Valid-SDK $_.DirectoryName } |
               Select-Object -First 1
        if ($hit) { return $hit.DirectoryName }
    }
    return $null
}

function Show-Msg($msg, $title) {
    Add-Type -AssemblyName System.Windows.Forms
    [System.Windows.Forms.MessageBox]::Show($msg, $title, 0, 48) | Out-Null
}

try {

    # ── Java check ──────────────────────────────────────────
    $javaOk = $false
    try {
        $psi = New-Object System.Diagnostics.ProcessStartInfo
        $psi.FileName               = "java"
        $psi.Arguments              = "-version"
        $psi.UseShellExecute        = $false
        $psi.RedirectStandardError  = $true
        $psi.RedirectStandardOutput = $true
        $psi.CreateNoWindow         = $true
        $p   = [System.Diagnostics.Process]::Start($psi)
        $out = $p.StandardError.ReadToEnd() + $p.StandardOutput.ReadToEnd()
        $p.WaitForExit()
        if ($out -match '"(\d+)') { $javaOk = ([int]$Matches[1] -ge 17) }
    } catch { $javaOk = $false }

    if (-not $javaOk) {
        Show-Msg "Java 17 or later is required.`nGet it free at https://adoptium.net" "BattleshipJava"
        exit 1
    }

    # ── JavaFX check / auto-download ────────────────────────
    $cacheFile = Join-Path $scriptDir ".javafx_path"
    $jfx = $null
    if (Test-Path $cacheFile) {
        $cached = (Get-Content $cacheFile -Raw).Trim()
        if (Test-Path (Join-Path $cached "javafx.controls.jar")) { $jfx = $cached }
    }
    if (-not $jfx) { $jfx = Find-JavaFX }
    if ($jfx) { $jfx | Set-Content $cacheFile }
    if (-not $jfx) {
        Add-Type -AssemblyName System.Windows.Forms
        $r = [System.Windows.Forms.MessageBox]::Show(
            "JavaFX SDK was not found.`nDownload it now? (~70 MB)",
            "BattleshipJava", 4, 32)
        if ($r -ne 6) { exit 1 }
        $dlDir = Join-Path $env:LOCALAPPDATA "javafx"
        $zip   = Join-Path $dlDir "openjfx.zip"
        New-Item -ItemType Directory -Force -Path $dlDir | Out-Null
        Invoke-WebRequest "https://download2.gluonhq.com/openjfx/21.0.3/openjfx-21.0.3_windows-x64_bin-sdk.zip" `
            -OutFile $zip -UseBasicParsing
        Expand-Archive $zip (Join-Path $dlDir "sdk") -Force
        Remove-Item $zip -ErrorAction SilentlyContinue
        $jfx = Find-JavaFX
        if (-not $jfx) { Show-Msg "JavaFX installation failed." "BattleshipJava"; exit 1 }
    }

    # ── Launch ──────────────────────────────────────────────
    Start-Process "java" -ArgumentList @(
        "--enable-native-access=javafx.graphics,javafx.media",
        "--module-path", "`"$jfx`"",
        "--add-modules", "javafx.controls,javafx.media",
        "-cp", "`"$outDir`"",
        "Main"
    )

} catch {
    Show-Msg "BattleshipJava failed to launch:`n`n$_" "BattleshipJava - Error"
}
