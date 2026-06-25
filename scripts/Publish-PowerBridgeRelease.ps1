[CmdletBinding()]
param(
[ValidatePattern('^\d+.\d+.\d+([.-][A-Za-z0-9.-]+)?$')]
[string]$Version,

```
[switch]$SkipBuild,

[switch]$AllowUnsignedAndroidArtifact
```

)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$repoRoot = Split-Path -Parent $PSScriptRoot
$versionFile = Join-Path $repoRoot 'VERSION'

if (-not $Version) {
if (-not (Test-Path -LiteralPath $versionFile)) {
throw "VERSION file was not found: $versionFile"
}

```
$Version = (Get-Content -LiteralPath $versionFile -Raw).Trim()
```

}

if (-not ($Version -match '^\d+.\d+.\d+([.-][A-Za-z0-9.-]+)?$')) {
throw "Invalid version value: $Version"
}

$repoName = 'Drakcain/PowerBridge'
$tagName = "v$Version"
$distRoot = Join-Path $repoRoot 'dist'

$apkPath = Join-Path $distRoot "PowerBridge-v$Version.apk"
$apkHashPath = "$apkPath.sha256"

$installerPath = Join-Path $distRoot "PowerBridge-Companion-Setup-v$Version.exe"
$installerHashPath = "$installerPath.sha256"

$androidMetadataPath = Join-Path $distRoot "PowerBridge-v$Version.android-build.json"

$ghCommand = Get-Command gh -ErrorAction SilentlyContinue
if (-not $ghCommand) {
throw 'GitHub CLI was not found. Install gh and authenticate before publishing.'
}

& gh auth status --repo $repoName
if ($LASTEXITCODE -ne 0) {
throw "GitHub CLI is not authenticated for $repoName."
}

if (-not $SkipBuild) {
$buildArgs = @(
'-NoProfile',
'-ExecutionPolicy',
'Bypass',
'-File',
(Join-Path $repoRoot 'scripts\Build-PowerBridgeReleaseAssets.ps1'),
'-Version',
$Version
)

```
if (-not $AllowUnsignedAndroidArtifact) {
    $buildArgs += '-RequireSignedAndroidRelease'
}

& pwsh @buildArgs
if ($LASTEXITCODE -ne 0) {
    throw 'Release asset build failed.'
}
```

}

$requiredAssets = @(
$apkPath,
$apkHashPath,
$installerPath,
$installerHashPath,
$androidMetadataPath
)

foreach ($required in $requiredAssets) {
if (-not (Test-Path -LiteralPath $required)) {
throw "Release asset was not found: $required"
}
}

$androidMetadata = Get-Content -LiteralPath $androidMetadataPath -Raw | ConvertFrom-Json

if (-not $AllowUnsignedAndroidArtifact) {
if ($androidMetadata.artifactKind -ne 'signed-release') {
throw "Android artifact is not a signed release build. Found artifactKind: $($androidMetadata.artifactKind). Configure local signing and rebuild before publishing."
}

```
if ($androidMetadata.releaseSigned -ne $true) {
    throw 'Android metadata does not confirm a signed release build. Configure local signing and rebuild before publishing.'
}
```

} else {
Write-Warning 'AllowUnsignedAndroidArtifact was specified. This release may contain a debug or unsigned Android artifact and must not be treated as an official production release.'
}

$apkHashActual = (Get-FileHash -LiteralPath $apkPath -Algorithm SHA256).Hash
$installerHashActual = (Get-FileHash -LiteralPath $installerPath -Algorithm SHA256).Hash

$apkHashFileText = Get-Content -LiteralPath $apkHashPath -Raw
$installerHashFileText = Get-Content -LiteralPath $installerHashPath -Raw

if ($apkHashFileText -notmatch [Regex]::Escape($apkHashActual)) {
throw "APK SHA-256 file does not match current APK: $apkHashPath"
}

if ($installerHashFileText -notmatch [Regex]::Escape($installerHashActual)) {
throw "Installer SHA-256 file does not match current installer: $installerHashPath"
}

$existingRelease = & gh release view $tagName --repo $repoName 2>$null
if ($LASTEXITCODE -eq 0) {
throw "GitHub release $tagName already exists."
}

$assetArgs = @(
"$apkPath#PowerBridge Android APK",
"$apkHashPath#PowerBridge Android APK SHA-256",
"$installerPath#PowerBridge Windows Companion Installer",
"$installerHashPath#PowerBridge Windows Companion Installer SHA-256",
"$androidMetadataPath#PowerBridge Android Build Metadata"
)

$releaseNotes = @"
PowerBridge $tagName release assets:

* Android APK
* Android APK SHA-256 checksum
* Windows Companion installer
* Windows Companion installer SHA-256 checksum
* Android build metadata

Android artifact kind: $($androidMetadata.artifactKind)
Android signed release: $($androidMetadata.releaseSigned)

The Android APK and Windows Companion installer are published as separate top-level assets.
"@

if ($AllowUnsignedAndroidArtifact) {
$releaseNotes += @"

WARNING:
This release was published with AllowUnsignedAndroidArtifact. The Android artifact may be unsigned or a debug fallback and should be treated as a validation/test release only.
"@
}

$createArgs = @(
'release',
'create',
$tagName
) + $assetArgs + @(
'--repo',
$repoName,
'--title',
"PowerBridge $tagName",
'--notes',
$releaseNotes,
'--target',
'main'
)

if ($AllowUnsignedAndroidArtifact) {
$createArgs += '--prerelease'
}

& gh @createArgs
if ($LASTEXITCODE -ne 0) {
throw "GitHub release creation failed for $tagName."
}

Write-Host "Published GitHub release: $tagName"
Write-Host "Repository: $repoName"
Write-Host "Android artifact kind: $($androidMetadata.artifactKind)"
Write-Host "Android signed release: $($androidMetadata.releaseSigned)"
Write-Host "APK: $apkPath"
Write-Host "APK SHA-256: $apkHashActual"
Write-Host "Installer: $installerPath"
Write-Host "Installer SHA-256: $installerHashActual"

if ($AllowUnsignedAndroidArtifact) {
Write-Warning 'Published release used AllowUnsignedAndroidArtifact and was marked as prerelease.'
}
