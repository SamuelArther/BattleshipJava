param()
$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$sourcePath  = Join-Path $projectRoot "Setup.cs"
$targetPath  = Join-Path $projectRoot "Setup.exe"
$csc         = "C:\Windows\Microsoft.NET\Framework64\v4.0.30319\csc.exe"

if (!(Test-Path $sourcePath)) { throw "Setup.cs not found: $sourcePath" }
if (!(Test-Path $csc))        { throw "C# compiler not found: $csc" }

Write-Host "Compiling Setup.exe..."
Remove-Item $targetPath -Force -ErrorAction SilentlyContinue

& $csc /nologo /target:winexe /out:$targetPath `
    /reference:System.dll `
    /reference:System.Windows.Forms.dll `
    /reference:System.Drawing.dll `
    $sourcePath

if ($LASTEXITCODE -ne 0) { throw "Compilation failed." }

$size = [math]::Round((Get-Item $targetPath).Length / 1KB, 0)
Write-Host "Done. Setup.exe created: $targetPath ($size KB)"
