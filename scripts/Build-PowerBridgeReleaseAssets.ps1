[CmdletBinding()]
param(
[ValidatePattern('^\d+.\d+.\d+([.-][A-Za-z0-9.-]+)?$')]
[string]$Version,

```
[switch]$RequireSignedAndroidRelease
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

$distRoot = Join-Path $repoRoot 'dist'
$androidRoot = Join-Path $repoRoot 'android'
$windowsCompanionRoot = Join-Path $repoRoot 'windows-companion'

$apkTarget = Join-Path $distRoot "PowerBridge-v$Version.apk"
$apkHashTarget = "$apkTarget.sha256"
$androidMetadataPath = Join-Path $distRoot "PowerBridge-v$Version.android-build.json"

$installerSource = Join-Path $windowsCompanionRoot "dist\PowerBridge-Companion-Setup-v$Version.exe"
$installerTarget = Join-Path $distRoot "PowerBridge-Companion-Setup-v$Version.exe"
$installerHashTarget = "$installerTarget.sha256"

$signedReleaseApk = Join-Path $androidRoot 'app\build\outputs\apk\release\app-release.apk'
$unsignedReleaseApk = Join-Path $androidRoot 'app\build\outputs\apk\release\app-release-unsigned.apk'
$debugApk = Join-Path $androidRoot 'app\build\outputs\apk\debug\app-debug.apk'

$signingPropertiesPath = Join-Path $androidRoot 'signing.properties'
$keystorePropertiesPath = Join-Path $androidRoot 'keystore.properties'
$signingPropertiesPresent = (
(Test-Path -LiteralPath $signingPropertiesPath) -or
(Test-Path -LiteralPath $keystorePropertiesPath)
)

New-Item -ItemType Directory -Force -Path $distRoot | Out-Null

$knownOutputs = @(
$apkTarget,
$apkHashTarget,
$androidMetadataPath,
$installerTarget,
$installerHashTarget
)

foreach ($path in $knownOutputs) {
Remove-Item -LiteralPath $path -Force -ErrorAction SilentlyContinue
}

Push-Location $androidRoot
try {
# Clean first so stale signed APKs cannot be mistaken for the current build output.
.\gradlew.bat clean :app:assembleRelease --no-daemon
if ($LASTEXITCODE -ne 0) {
throw 'Android release build failed.'
}

```
if ((-not $RequireSignedAndroidRelease) -and (-not (Test-Path -LiteralPath $signedReleaseApk))) {
    .\gradlew.bat :app:assembleDebug --no-daemon
    if ($LASTEXITCODE -ne 0) {
        throw 'Android debug build failed.'
    }
}
```

} finally {
Pop-Location
}

if (Test-Path -LiteralPath $signedReleaseApk) {
$apkSource = $signedReleaseApk
$androidArtifactKind = 'signed-release'
$androidReleaseSigned = $true
} elseif ($RequireSignedAndroidRelease) {
$guidance = if ($signingPropertiesPresent) {
'Signing properties were detected, but no signed release APK was produced. Check android/signing.properties or android/keystore.properties and verify the keystore path, alias, and passwords locally.'
} else {
'No local signing properties were detected. Copy android/signing.properties.example to android/signing.properties and keep the real keystore outside the repository.'
}

```
throw "Signed Android release APK was required but not produced. $guidance"
```

} elseif (Test-Path -LiteralPath $debugApk) {
$apkSource = $debugApk
$androidArtifactKind = 'debug-fallback'
$androidReleaseSigned = $false

```
Write-Warning 'No signed Android release APK was produced. Falling back to the debug APK for local validation only. This is not an official production-signed release build.'
```

} else {
throw "Android APK was not found. Checked signed release at $signedReleaseApk and debug fallback at $debugApk"
}

Copy-Item -LiteralPath $apkSource -Destination $apkTarget -Force

$apkHash = Get-FileHash -LiteralPath $apkTarget -Algorithm SHA256
Set-Content -LiteralPath $apkHashTarget -Encoding UTF8 -Value "$($apkHash.Hash)  $(Split-Path -Leaf $apkTarget)"

$apkSourceRelative = Resolve-Path -LiteralPath $apkSource -Relative
$apkTargetRelative = Resolve-Path -LiteralPath $apkTarget -Relative

$androidBuildMetadata = [ordered]@{
version = $Version
artifactKind = $androidArtifactKind
releaseSigned = $androidReleaseSigned
signingPropertiesDetected = $signingPropertiesPresent
sourceRelativePath = $apkSourceRelative
outputRelativePath = $apkTargetRelative
sha256 = $apkHash.Hash
generatedAtUtc = [DateTime]::UtcNow.ToString('o')
}

$androidBuildMetadata |
ConvertTo-Json -Depth 8 |
Set-Content -LiteralPath $androidMetadataPath -Encoding UTF8

$installerBuildScript = Join-Path $windowsCompanionRoot 'scripts\Build-PowerBridgeCompanionInstaller.ps1'
if (-not (Test-Path -LiteralPath $installerBuildScript)) {
throw "Windows Companion installer build script was not found: $installerBuildScript"
}

& pwsh -NoProfile -ExecutionPolicy Bypass -File $installerBuildScript -Version $Version
if ($LASTEXITCODE -ne 0) {
throw 'PowerBridge Windows Companion installer build failed.'
}

if (-not (Test-Path -LiteralPath $installerSource)) {
throw "Windows Companion installer was not found after build: $installerSource"
}

Copy-Item -LiteralPath $installerSource -Destination $installerTarget -Force

$installerHash = Get-FileHash -LiteralPath $installerTarget -Algorithm SHA256
Set-Content -LiteralPath $installerHashTarget -Encoding UTF8 -Value "$($installerHash.Hash)  $(Split-Path -Leaf $installerTarget)"

Write-Host "Built Android APK: $apkTarget"
Write-Host "Android artifact kind: $androidArtifactKind"
Write-Host "Android signed release: $androidReleaseSigned"
Write-Host "Android SHA-256: $apkHashTarget"
Write-Host "Android build metadata: $androidMetadataPath"
Write-Host "Built Windows installer: $installerTarget"
Write-Host "Windows installer SHA-256: $installerHashTarget"

if ($androidArtifactKind -ne 'signed-release') {
Write-Warning 'Android output is not a signed release APK. Do not publish it as an official public Android release asset.'
}
