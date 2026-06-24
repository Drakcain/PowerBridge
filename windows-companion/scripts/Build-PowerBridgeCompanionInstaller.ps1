[CmdletBinding()]
param(
    [ValidatePattern('^\d+\.\d+\.\d+([.-][A-Za-z0-9.-]+)?$')]
    [string]$Version
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$companionRoot = Split-Path -Parent $PSScriptRoot
$repoRoot = Split-Path -Parent $companionRoot
$versionFile = Join-Path $repoRoot 'VERSION'

if (-not $Version) {
    if (-not (Test-Path -LiteralPath $versionFile)) {
        throw "VERSION file was not found: $versionFile"
    }
    $Version = (Get-Content -LiteralPath $versionFile -Raw).Trim()
}

function Find-InnoCompiler {
    $candidates = @(
        (Join-Path $env:LOCALAPPDATA 'Programs\Inno Setup 6\ISCC.exe'),
        'C:\Program Files (x86)\Inno Setup 6\ISCC.exe',
        'C:\Program Files\Inno Setup 6\ISCC.exe'
    )
    foreach ($candidate in $candidates) {
        if (Test-Path -LiteralPath $candidate) {
            return $candidate
        }
    }
    $command = Get-Command 'ISCC.exe' -ErrorAction SilentlyContinue
    if ($command) {
        return $command.Source
    }
    throw 'Inno Setup 6 compiler was not found.'
}

function Remove-PathIfExists {
    param([Parameter(Mandatory)][string]$Path)
    if (Test-Path -LiteralPath $Path) {
        Remove-Item -LiteralPath $Path -Recurse -Force
    }
}

$buildRoot = Join-Path $companionRoot 'build'
$payloadRoot = Join-Path $buildRoot 'payload'
$distRoot = Join-Path $companionRoot 'dist'
$installerScript = Join-Path $companionRoot 'installer\PowerBridgeCompanion.iss'
$iconPath = Join-Path $companionRoot 'assets\icon\PowerBridgeCompanion.ico'

foreach ($requiredPath in @(
    $installerScript,
    $iconPath,
    (Join-Path $companionRoot 'lib\QRCoder.dll'),
    (Join-Path $companionRoot 'src\PowerBridgeCompanion.ps1'),
    (Join-Path $companionRoot 'src\PowerBridgeCompanion.Gui.ps1'),
    (Join-Path $companionRoot 'scripts\Start-PowerBridgeCompanionGui.ps1'),
    (Join-Path $companionRoot 'scripts\Update-PowerBridgeCompanion.ps1'),
    (Join-Path $companionRoot 'README.md'),
    (Join-Path $repoRoot 'VERSION')
)) {
    if (-not (Test-Path -LiteralPath $requiredPath)) {
        throw "Required companion release input was not found: $requiredPath"
    }
}

& powershell.exe -NoProfile -ExecutionPolicy Bypass -File (Join-Path $companionRoot 'scripts\Test-PowerBridgeCompanion.ps1')
if ($LASTEXITCODE -ne 0) {
    throw 'PowerBridge Companion validation failed.'
}

$resolvedBuildRoot = [System.IO.Path]::GetFullPath($buildRoot)
if (-not $resolvedBuildRoot.StartsWith([System.IO.Path]::GetFullPath($companionRoot), [System.StringComparison]::OrdinalIgnoreCase)) {
    throw "Unsafe build path: $resolvedBuildRoot"
}

Remove-PathIfExists -Path $buildRoot
New-Item -ItemType Directory -Force -Path $payloadRoot, $distRoot | Out-Null
Copy-Item -LiteralPath (Join-Path $companionRoot 'README.md') -Destination (Join-Path $payloadRoot 'README.md') -Force
Copy-Item -LiteralPath (Join-Path $repoRoot 'VERSION') -Destination (Join-Path $payloadRoot 'VERSION') -Force
Copy-Item -LiteralPath (Join-Path $repoRoot 'LICENSE') -Destination (Join-Path $payloadRoot 'LICENSE') -Force
Copy-Item -LiteralPath (Join-Path $repoRoot 'INSTALL-NOTICE.txt') -Destination (Join-Path $payloadRoot 'INSTALL-NOTICE.txt') -Force
Copy-Item -LiteralPath (Join-Path $repoRoot 'THIRD-PARTY-NOTICES.md') -Destination (Join-Path $payloadRoot 'THIRD-PARTY-NOTICES.md') -Force
Copy-Item -LiteralPath (Join-Path $repoRoot 'SIGNING.md') -Destination (Join-Path $payloadRoot 'SIGNING.md') -Force
Copy-Item -Path (Join-Path $companionRoot 'src') -Destination (Join-Path $payloadRoot 'src') -Recurse -Force
Copy-Item -Path (Join-Path $companionRoot 'scripts') -Destination (Join-Path $payloadRoot 'scripts') -Recurse -Force
Copy-Item -Path (Join-Path $companionRoot 'lib') -Destination (Join-Path $payloadRoot 'lib') -Recurse -Force
Copy-Item -Path (Join-Path $companionRoot 'assets') -Destination (Join-Path $payloadRoot 'assets') -Recurse -Force
Remove-PathIfExists -Path (Join-Path $payloadRoot 'output')
New-Item -ItemType Directory -Force -Path (Join-Path $payloadRoot 'output') | Out-Null

$iscc = Find-InnoCompiler
& $iscc "/DMyAppVersion=$Version" "/DMyPayloadDir=$payloadRoot" $installerScript | Out-Host
if ($LASTEXITCODE -ne 0) {
    throw "Inno Setup failed with exit code $LASTEXITCODE."
}

$output = Join-Path $distRoot "PowerBridge-Companion-Setup-v$Version.exe"
if (-not (Test-Path -LiteralPath $output)) {
    throw "Expected installer output was not created: $output"
}

Write-Host "Built: $output"
