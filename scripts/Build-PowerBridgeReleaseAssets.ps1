[CmdletBinding()]
param(
    [ValidatePattern('^\d+\.\d+\.\d+([.-][A-Za-z0-9.-]+)?$')]
    [string]$Version,
    [switch]$RequireSignedAndroidRelease
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
$apkTarget = Join-Path $distRoot "PowerBridge-v$Version.apk"
$androidMetadataPath = Join-Path $distRoot "PowerBridge-v$Version.android-build.json"
$androidRoot = Join-Path $repoRoot 'android'
$signedReleaseApk = Join-Path $androidRoot 'app\build\outputs\apk\release\app-release.apk'
$unsignedReleaseApk = Join-Path $androidRoot 'app\build\outputs\apk\release\app-release-unsigned.apk'
$debugApk = Join-Path $androidRoot 'app\build\outputs\apk\debug\app-debug.apk'
$signingPropertiesPath = Join-Path $androidRoot 'signing.properties'
$keystorePropertiesPath = Join-Path $androidRoot 'keystore.properties'

New-Item -ItemType Directory -Force -Path $distRoot | Out-Null
Remove-Item -LiteralPath $apkTarget -Force -ErrorAction SilentlyContinue
Remove-Item -LiteralPath $androidMetadataPath -Force -ErrorAction SilentlyContinue

$signingPropertiesPresent = (Test-Path -LiteralPath $signingPropertiesPath) -or (Test-Path -LiteralPath $keystorePropertiesPath)

Push-Location $androidRoot
try {
    .\gradlew.bat assembleRelease --no-daemon
    if ($LASTEXITCODE -ne 0) {
        throw 'Android release build failed.'
    }

    if ((-not $RequireSignedAndroidRelease) -and (-not (Test-Path -LiteralPath $signedReleaseApk))) {
        .\gradlew.bat assembleDebug --no-daemon
        if ($LASTEXITCODE -ne 0) {
            throw 'Android debug build failed.'
        }
    }
} finally {
    Pop-Location
}

if (Test-Path -LiteralPath $signedReleaseApk) {
    $apkSource = $signedReleaseApk
    $androidArtifactKind = 'signed-release'
} elseif ($RequireSignedAndroidRelease) {
    $guidance = if ($signingPropertiesPresent) {
        'Signing properties were detected, but no signed release APK was produced. Check android/signing.properties or android/keystore.properties and verify the keystore path/passwords locally.'
    } else {
        'No local signing properties were detected. Create android/signing.properties from android/signing.properties.example and keep the real keystore outside the repository.'
    }
    throw "Signed Android release APK was required but not produced. $guidance"
} elseif (Test-Path -LiteralPath $debugApk) {
    $apkSource = $debugApk
    $androidArtifactKind = 'debug-fallback'
    Write-Warning 'No signed Android release APK was produced. Falling back to the debug APK for local validation only. This is not an official production-signed release build.'
} else {
    throw "Android APK was not found. Checked signed release at $signedReleaseApk and debug fallback at $debugApk"
}

Copy-Item -LiteralPath $apkSource -Destination $apkTarget -Force

$androidBuildMetadata = [ordered]@{
    version = $Version
    artifactKind = $androidArtifactKind
    sourcePath = $apkSource
    signingPropertiesDetected = $signingPropertiesPresent
    generatedAtUtc = [DateTime]::UtcNow.ToString('o')
}
$androidBuildMetadata | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath $androidMetadataPath -Encoding UTF8

& powershell.exe -NoProfile -ExecutionPolicy Bypass -File (Join-Path $repoRoot 'windows-companion\scripts\Build-PowerBridgeCompanionInstaller.ps1') -Version $Version
if ($LASTEXITCODE -ne 0) {
    throw 'PowerBridge Windows Companion installer build failed.'
}

Write-Host "Built Android APK: $apkTarget"
Write-Host "Android artifact kind: $androidArtifactKind"
Write-Host "Android build metadata: $androidMetadataPath"
Write-Host "Built Windows installer: $(Join-Path $repoRoot "windows-companion\dist\PowerBridge-Companion-Setup-v$Version.exe")"
