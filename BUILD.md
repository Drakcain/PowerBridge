# Build Notes

This repository contains the public PowerBridge project.

Current public components:

* `android/` — Android client
* `windows-companion/` — Windows setup helper

PowerBridge uses user-owned configuration and user-owned wake paths. It does not ship with a shared relay service, shared token, or developer-managed wake infrastructure.

Generated artifacts, diagnostics ZIPs, QR images, setup JSON payloads, logs, caches, keystores, signing files, and local-only machine output must not be committed.

## Current Product Scope

PowerBridge is focused on wake/boot attempts only.

Active scope:

* Local Wi-Fi Wake
* Old Android Device relay path
* My Own Server / Home Relay Server path
* Alexa / Google Voice Devices as a future smart-home path

Removed from active scope:

* TV / streaming relay
* Fire TV / Android TV relay
* Roku relay
* smart plug / hardware power-cycle boot
* router / VPN as a standalone method
* remote shutdown
* remote restart
* Windows admin credential control

## Android

### Requirements

* Windows with PowerShell
* Android SDK
* JDK 21

### Build Debug APK

```powershell
Set-Location "<repo-root>\android"
.\gradlew.bat :app:assembleDebug --no-daemon
```

Debug APK output is for testing only. It is not production-signed.

### Build Release APK

```powershell
Set-Location "<repo-root>\android"
.\gradlew.bat :app:assembleRelease --no-daemon
```

`assembleRelease` may complete without local signing. That is acceptable for CI/build validation, but an unsigned release APK is not an official public release artifact.

### Android Signing Notes

* `local.properties` is machine-local and must not be committed.
* Release signing must use a private keystore outside the repository.
* Debug builds are not production-signed.
* Unsigned release builds are for validation only.
* Official public Android releases should use a locally signed release APK.
* Local signing properties belong in `android/signing.properties` or `android/keystore.properties`.
* `android/signing.properties` and `android/keystore.properties` must stay ignored by Git.
* Keystores must never be committed.

### Optional Local Signing Setup

```powershell
Set-Location "<repo-root>\android"
Copy-Item ".\signing.properties.example" ".\signing.properties"
notepad ".\signing.properties"
```

### Build Local Signed Release

```powershell
Set-Location "<repo-root>\android"
.\gradlew.bat clean :app:assembleRelease --no-daemon
```

### Verify Signed APK

Adjust the build-tools path to match the installed Android SDK.

```powershell
& "<android-sdk>\build-tools\34.0.0\apksigner.bat" verify --verbose ".\app\build\outputs\apk\release\app-release.apk"
```

### Generate SHA-256 Hash

```powershell
Get-FileHash ".\app\build\outputs\apk\release\app-release.apk" -Algorithm SHA256
```

## Windows Companion

The Windows Companion is a setup helper. It detects this PC, generates setup data, and helps the Android app import a PC profile.

The Windows Companion does not wake the PC directly.

### Run Validation

```powershell
Set-Location "<repo-root>\windows-companion"
pwsh -NoProfile -ExecutionPolicy Bypass -File ".\scripts\Test-PowerBridgeCompanion.ps1"
```

### Run CLI Detection Without Clipboard Dependency

```powershell
Set-Location "<repo-root>\windows-companion"
pwsh -NoProfile -ExecutionPolicy Bypass -File ".\src\PowerBridgeCompanion.ps1" -NoClipboard
```

### Open the GUI

```powershell
Set-Location "<repo-root>\windows-companion"
pwsh -NoProfile -ExecutionPolicy Bypass -File ".\scripts\Start-PowerBridgeCompanionGui.ps1"
```

### Windows Companion Notes

* GUI QR output is local/session-oriented.
* Generated output in `windows-companion/output/` is ignored and should stay local.
* Generated setup JSON files may contain real local network values.
* Generated QR images may contain real local setup values.
* Do not commit generated QR images, setup JSON payloads, screenshots, logs, or diagnostics.
* During early public releases, the Windows Companion may be distributed as an unsigned script, unsigned installer, or unsigned package.
* Windows SmartScreen or “unknown publisher” prompts may appear if a signed Windows package is not available yet.

## Future Release Packaging

The public GitHub release page should publish flat release assets directly.

Recommended public release assets:

* Android APK
* Windows Companion installer

Recommended asset naming:

```text
PowerBridge-vX.Y.Z.apk
PowerBridge-Companion-Setup-vX.Y.Z.exe
```

### Windows Installer Behavior

Recommended Windows installer behavior:

* build a normal installer executable, not a loose script bundle
* default local install target under `C:\Tools`
* keep the install path stable and operator-readable
* do not require users to unpack a companion ZIP just to launch the GUI

### Release Page Rule

Upload the APK and installer directly as separate release assets.

Do not place them inside an additional release folder or archive unless there is a real technical reason.

## Local Release Asset Build

```powershell
Set-Location "<repo-root>"
pwsh -NoProfile -ExecutionPolicy Bypass -File ".\scripts\Build-PowerBridgeReleaseAssets.ps1"
```

Release asset behavior:

* if local Android signing is configured, the release asset script should prefer the signed release APK
* if local Android signing is not configured, the release asset script may fall back to a debug APK for local validation only
* fallback to a debug APK must warn clearly
* official GitHub release publication should use a locally signed Android release build

## GitHub Release Publishing

```powershell
Set-Location "<repo-root>"
pwsh -NoProfile -ExecutionPolicy Bypass -File ".\scripts\Publish-PowerBridgeRelease.ps1"
```

Release publishing rules:

* do not publish unsigned/debug Android artifacts by default
* unsigned/debug Android artifacts should be blocked unless an explicit override is used
* any override must be obvious and intentional
* do not publish `v1.0.0` until all selected final methods are working, tested, documented, packaged, and consumer-ready

## Repo Hygiene Checks

Run these before committing or publishing.

### Check Working Tree

```powershell
Set-Location "<repo-root>"
git status
git diff --cached --name-only
```

### Public-Value Sweep

```powershell
Set-Location "<repo-root>"
rg -n --glob '!android/app/build/**' --glob '!android/build/**' --glob '!android/.gradle/**' --glob '!windows-companion/output/**' --glob '!dist/**' --glob '!**/*.apk' --glob '!**/*.aab' --glob '!**/*.zip' --glob '!**/*.png' --glob '!**/*.jpg' --glob '!**/*.jpeg' --glob '!**/*.exe' --glob '!**/*.sha256' "(legacy donor app|developer-specific infrastructure|private relay assumptions|relayToken|Bearer sk-|sk-proj-|sk-[A-Za-z0-9]{20,}|[A-Za-z]:\\\\Users\\\\|/Users/|WakeMyPC|wakemypc|Dell|Ryan|Drak|draks-world|192\.168\.0\.85|192\.168\.0\.60|192\.168\.0\.255|wake\.draks-world\.dedyn\.io|C8:7F:54:02:97:9D|storePassword|keyPassword|\.jks|\.keystore|signing\.properties|keystore\.properties)" .
```

### Before Commit, Verify

* no keystores are staged
* no signing keys are staged
* no signing property files are staged
* no `local.properties` files are staged
* no generated diagnostics ZIPs are staged
* no generated QR images are staged
* no generated setup JSON output is staged
* no screenshots with real local values are staged
* no logs with local network details are staged
* no generated APK, AAB, EXE, ZIP, or checksum artifacts are staged unless they are intentional public release assets

## Do Not Commit

Do not commit:

* `local.properties`
* `android/signing.properties`
* `android/keystore.properties`
* keystores
* signing keys
* passwords
* tokens
* generated diagnostics ZIPs
* generated QR images with real values
* generated setup JSON payloads with real values
* local screenshots with real IP addresses, MAC addresses, tokens, or endpoints
* Gradle build folders
* caches
* logs
* generated APKs / AABs
* generated EXEs / ZIPs
* generated release artifacts unless intentionally publishing a release

## Added Recommendations

Before a public release:

* build and verify the Android APK
* use a signed Android release APK for official release
* run the Windows Companion validation script
* build a normal Windows Companion installer
* generate SHA-256 hashes for final release assets
* verify release asset names
* verify that `INSTALL-NOTICE.txt` is current
* verify GitHub Actions are green
* run the public-value sweep
* verify no generated local setup data is staged
* verify no signing material is staged
* test a clean Android install
* test Windows Companion QR generation
* test Android QR import
* test Local Wi-Fi Wake
* test any selected remote wake method before claiming it is ready
* do not publish `v1.0.0` until every selected final method is consumer-ready

## Release Readiness Rule

PowerBridge `v1.0.0` is reserved for a complete consumer-ready release.

Do not use `v1.0.0` for a partial beta, method preview, local-only test build, or incomplete relay implementation.

A final `v1.0.0` release should have:

* signed Android release APK
* Windows Companion installer
* clear install notice
* clean build notes
* accurate README
* release checklist completed
* public-value sweep completed
* no private local values
* no generated diagnostics or QR payloads
* all selected final methods working and documented
