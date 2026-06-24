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
PowerBridge-v0.6.apk
PowerBridge-Companion-Setup-v0.6.exe
```

Recommended Windows installer behavior:

* build a normal installer executable, not a loose script bundle
* default local install target under `C:\Tools`
* keep the install path stable and operator-readable
* do not require users to unpack a companion ZIP just to launch the GUI

Release-page rule:

* upload the APK and installer directly as separate release assets
* do not place them inside an additional release folder/archive unless there is a real technical reason

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
