[CmdletBinding()]
param(
    [ValidatePattern('^\d+\.\d+\.\d+([.-][A-Za-z0-9.-]+)?$')]
    [string]$Version,
    [switch]$SkipBuild,
    [switch]$AllowUnsignedAndroidArtifact
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

if (-not $SkipBuild) {
    $buildArgs = @(
        '-NoProfile',
        '-ExecutionPolicy',
        'Bypass',
        '-File',
        (Join-Path $repoRoot 'scripts\Build-PowerBridgeReleaseAssets.ps1'),
        '-Version',
        $Version,
    )
    if (-not $AllowUnsignedAndroidArtifact) {
        $buildArgs += '-RequireSignedAndroidRelease'
    }
    & powershell.exe @buildArgs
    if ($LASTEXITCODE -ne 0) {
        throw 'Release asset build failed.'
    }
}

$tagName = "v$Version"
$apkPath = Join-Path $repoRoot "dist\PowerBridge-v$Version.apk"
$installerPath = Join-Path $repoRoot "windows-companion\dist\PowerBridge-Companion-Setup-v$Version.exe"
$androidMetadataPath = Join-Path $repoRoot "dist\PowerBridge-v$Version.android-build.json"
foreach ($required in @($apkPath, $installerPath)) {
    if (-not (Test-Path -LiteralPath $required)) {
        throw "Release asset was not found: $required"
    }
}

if (-not $AllowUnsignedAndroidArtifact) {
    if (-not (Test-Path -LiteralPath $androidMetadataPath)) {
        throw "Android build metadata was not found: $androidMetadataPath. Rebuild release assets locally before publishing."
    }
    $androidMetadata = Get-Content -LiteralPath $androidMetadataPath -Raw | ConvertFrom-Json
    if ($androidMetadata.artifactKind -ne 'signed-release') {
        throw "Android artifact is not a signed release build (found: $($androidMetadata.artifactKind)). Configure local signing and rebuild before publishing."
    }
}

$existing = gh release view $tagName --repo Drakcain/PowerBridge 2>$null
if ($LASTEXITCODE -eq 0) {
    throw "GitHub release $tagName already exists."
}

gh release create $tagName `
    "$apkPath#PowerBridge Android APK" `
    "$installerPath#PowerBridge Windows Companion Installer" `
    --repo Drakcain/PowerBridge `
    --title "PowerBridge $tagName" `
    --notes "PowerBridge $tagName ships the Android APK and the Windows Companion installer as separate top-level assets."

if ($LASTEXITCODE -ne 0) {
    throw "GitHub release creation failed for $tagName."
}
