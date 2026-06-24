# Build Notes

This repository contains two public PowerBridge components:

* `android/` - Android client
* `windows-companion/` - Windows setup helper

Generated artifacts, diagnostics ZIPs, QR images, JSON payloads, logs, caches, keystores, and local-only machine output must not be committed.

## Android

Requirements:

* Windows with PowerShell
* Android SDK
* JDK 21

Build debug:

```powershell
Set-Location "<repo-root>\android"
.\gradlew.bat assembleDebug --no-daemon
```

Build release:

```powershell
Set-Location "<repo-root>\android"
.\gradlew.bat assembleRelease --no-daemon
```

Notes:

* `local.properties` is machine-local and must not be committed.
* Release signing must use a private keystore outside the repository.
* Debug builds are not production-signed.
* `assembleRelease` can complete without local signing, which is acceptable for CI validation but not for an official signed public APK.
* Local signing properties belong in `android/signing.properties` or `android/keystore.properties`, both ignored by Git.

Optional local signing setup:

```powershell
Set-Location "<repo-root>\android"
Copy-Item ".\signing.properties.example" ".\signing.properties"
notepad ".\signing.properties"
```

Local signed release build:

```powershell
Set-Location "<repo-root>\android"
.\gradlew.bat clean assembleRelease --no-daemon
```

Signed APK verification example:

```powershell
& "<android-sdk>\build-tools\34.0.0\apksigner.bat" verify --verbose ".\app\build\outputs\apk\release\app-release.apk"
```

SHA-256 example:

```powershell
Get-FileHash ".\app\build\outputs\apk\release\app-release.apk" -Algorithm SHA256
```

## Windows Companion

Run the validation script:

```powershell
Set-Location "<repo-root>\windows-companion"
pwsh -NoProfile -ExecutionPolicy Bypass -File ".\scripts\Test-PowerBridgeCompanion.ps1"
```

Run CLI detection without clipboard dependency:

```powershell
Set-Location "<repo-root>\windows-companion"
pwsh -NoProfile -ExecutionPolicy Bypass -File ".\src\PowerBridgeCompanion.ps1" -NoClipboard
```

Open the GUI:

```powershell
Set-Location "<repo-root>\windows-companion"
pwsh -NoProfile -ExecutionPolicy Bypass -File ".\scripts\Start-PowerBridgeCompanionGui.ps1"
```

Notes:

* GUI QR output is session-local.
* Generated output in `windows-companion/output/` is ignored and should stay local.
* The companion is setup-only and does not wake the PC directly.

## Future Release Packaging

The public release page should publish two flat assets:

* Android APK
* Windows Companion installer

Recommended asset naming:

```text
PowerBridge-vX.Y.Z.apk
PowerBridge-Companion-Setup-vX.Y.Z.exe
```

Recommended Windows installer behavior:

* build a normal installer executable, not a loose script bundle
* default local install target under `C:\Tools`
* keep the install path stable and operator-readable
* do not require users to unpack a companion ZIP just to launch the GUI

Release-page rule:

* upload the APK and installer directly as separate release assets
* do not place them inside an additional release folder/archive unless there is a real technical reason

Local release asset build:

```powershell
Set-Location "<repo-root>"
pwsh -NoProfile -ExecutionPolicy Bypass -File ".\scripts\Build-PowerBridgeReleaseAssets.ps1"
```

Release script truth:

* if local Android signing is configured, the release asset script prefers the signed release APK
* if local Android signing is not configured, the release asset script can fall back to the debug APK for local validation and warns clearly
* official GitHub release publication should use a locally signed Android release build
* `scripts\Publish-PowerBridgeRelease.ps1` now blocks publishing unsigned/debug Android artifacts unless explicitly overridden

Local GitHub release publish:

```powershell
Set-Location "<repo-root>"
pwsh -NoProfile -ExecutionPolicy Bypass -File ".\scripts\Publish-PowerBridgeRelease.ps1"
```

## Repo Hygiene Checks

Recommended pre-publish checks:

```powershell
Set-Location "<repo-root>"
git status
git diff --cached --name-only
```

Run a public-value sweep:

```powershell
rg -n --glob '!android/app/build/**' --glob '!android/build/**' --glob '!android/.gradle/**' --glob '!windows-companion/output/**' --glob '!**/*.apk' --glob '!**/*.aab' --glob '!**/*.zip' --glob '!**/*.png' --glob '!**/*.jpg' --glob '!**/*.jpeg' "(legacy donor app|developer-specific infrastructure|private relay assumptions|relayToken|Bearer sk-|sk-proj-|sk-[A-Za-z0-9]{20,}|[A-Za-z]:\\\\Users\\\\|/Users/)" .
```

Before commit, verify:

* no keystores are staged
* no signing keys are staged
* no generated diagnostics ZIPs are staged
* no generated QR or JSON output is staged
* no local screenshots with real values are staged
