[CmdletBinding()]
param(
    [ValidatePattern('^\d+\.\d+\.\d+([.-][A-Za-z0-9.-]+)?$')]
    [string]$Version
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$repoRoot = Split-Path -Parent $PSScriptRoot
$versionFile = Join-Path $repoRoot 'VERSION'
if (-not $Version) {
    if (-not (Test-Path -LiteralPath $versionFile)) {
        throw "VERSION file was not found: $versionFile"
    }
    $Version = (Get-Content -LiteralPath $versionFile -Raw).Trim()
}

$distRoot = Join-Path $repoRoot 'dist'
$apkSource = Join-Path $repoRoot 'android\app\build\outputs\apk\debug\app-debug.apk'
$apkTarget = Join-Path $distRoot "PowerBridge-v$Version.apk"

New-Item -ItemType Directory -Force -Path $distRoot | Out-Null
Remove-Item -LiteralPath $apkTarget -Force -ErrorAction SilentlyContinue

Push-Location (Join-Path $repoRoot 'android')
try {
    .\gradlew.bat assembleDebug --no-daemon
    if ($LASTEXITCODE -ne 0) {
        throw 'Android debug build failed.'
    }
} finally {
    Pop-Location
}

if (-not (Test-Path -LiteralPath $apkSource)) {
    throw "Android APK was not found: $apkSource"
}

Copy-Item -LiteralPath $apkSource -Destination $apkTarget -Force

& powershell.exe -NoProfile -ExecutionPolicy Bypass -File (Join-Path $repoRoot 'windows-companion\scripts\Build-PowerBridgeCompanionInstaller.ps1') -Version $Version
if ($LASTEXITCODE -ne 0) {
    throw 'PowerBridge Windows Companion installer build failed.'
}

Write-Host "Built Android APK: $apkTarget"
Write-Host "Built Windows installer: $(Join-Path $repoRoot "windows-companion\dist\PowerBridge-Companion-Setup-v$Version.exe")"
