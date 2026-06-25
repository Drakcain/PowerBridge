# PowerBridge

![PowerBridge Brand Banner](assets/branding/powerbridge-brand-banner.png)

PowerBridge is a public Wake-on-LAN setup and wake/boot project. It includes an Android client, saved PC profiles, QR-assisted setup, diagnostics export, and user-owned wake paths for local and remote wake attempts.

PowerBridge is designed for normal users first: scan a setup QR, confirm the PC, choose where wake should work, and see what is ready versus what still needs setup.

PowerBridge does **not** ship with a shared public relay service, shared token, developer-managed wake server, private relay defaults, private domains, private IP addresses, private MAC addresses, or personal setup assumptions.

## Public Components

This repository contains two public PowerBridge components:

* `android/` — Android client
* `windows-companion/` — Windows setup helper

The Windows Companion helps detect local PC/network setup values and generates scan-safe QR setup payloads for Android import. It does **not** wake the PC directly.

## Latest Public Release

```text
v0.6.1
```

## Current Development Version

```text
0.7.7
```

## What Works Now

PowerBridge currently supports:

* saved PC profiles
* `Local Wi-Fi Wake`
* `My Own Server / Home Relay Server` for advanced user-owned relay setups
* Windows Companion QR setup import
* guided wake setup
* plain-language wake-path selection
* local setup checks
* diagnostics generation
* user-initiated diagnostics ZIP sharing

## Current Prototype / Planned Work

PowerBridge is currently building toward the approved remote wake model:

* `Old Android Device` relay path
* `My Own Server / Home Relay Server` path
* `Alexa / Google Voice Devices` as a future smart-home path

The next concrete runtime target is the `Old Android Device` relay path.

## Not Supported

PowerBridge does not currently support:

* shutdown
* restart
* hibernate
* remote desktop control
* target-side command execution
* Windows admin credential control
* built-in cloud backend
* shared public relay backend
* shared relay token
* built-in smart plug wake
* built-in TV / streaming relay wake
* built-in Roku relay wake
* router / VPN as a standalone method

## Removed From Active Scope

These method families were removed from the active PowerBridge roadmap because they were unreliable, not true wake relays, too vendor-specific, or likely to cause product drift:

* TV / streaming relay
* Fire TV / Android TV relay
* Roku relay
* smart plug / hardware power-cycle boot
* router / VPN as a standalone method
* hardware power-cycle workflow

PowerBridge is focused on reliable wake/boot attempts through user-owned devices that can realistically be available inside the home network.

## Architecture Status

* `Phase 14B` added guided wake setup and plain-language method selection.
* `Phase 14C` completed honest wake-path readiness wiring for `Home Wi-Fi only`, `Away from home`, and `Not sure`.
* `Phase 14C.1` polished guided setup completion, Home Relay report sharing, CI path filters, and phase-aligned versioning.
* `Phase 14D` completed the main-APK method guide and readiness realignment.
* `Phase 14E` stripped unreliable method families and reset the remote model to three approved paths.
* `Phase 14F` is the next runtime foundation phase for the `Old Android Device` relay path.

See:

* [docs/HOME-DEVICE-RELAY-ARCHITECTURE.md](docs/HOME-DEVICE-RELAY-ARCHITECTURE.md)
* [docs/HOME-DEVICE-RELAY-CONTRACTS.md](docs/HOME-DEVICE-RELAY-CONTRACTS.md)
* [docs/HOME-DEVICE-RELAY-PROTOTYPE-PLAN.md](docs/HOME-DEVICE-RELAY-PROTOTYPE-PLAN.md)
* [docs/POWERBRIDGE-ROADMAP.md](docs/POWERBRIDGE-ROADMAP.md)
* [docs/WAKE-METHODS.md](docs/WAKE-METHODS.md)
* [docs/README.md](docs/README.md)

## Consumer Setup Flow

PowerBridge should continue to feel like a simple consumer setup product:

1. Install PowerBridge on Android.
2. Run PowerBridge Windows Companion on the PC.
3. Scan the setup QR on Android.
4. Confirm the detected PC.
5. Choose where wake should work.
6. See what is ready now versus what still needs setup.
7. Use Wake or Boot from the main screen.

The app should not require normal users to understand low-level networking terms during initial setup.

## Repository Layout

```text
PowerBridge/
  .github/workflows/
  android/
  windows-companion/
  assets/
  docs/
  scripts/
  BUILD.md
  INSTALL-NOTICE.txt
  LICENSE
  README.md
  SECURITY.md
  SIGNING.md
  THIRD-PARTY-NOTICES.md
  VERSION
```

## Product Boundary

PowerBridge is generic public software.

It does not include:

* developer-specific infrastructure
* private relay defaults
* private domains
* private IP addresses
* private MAC addresses
* private tokens
* personal setup assumptions
* shared hosted relay infrastructure

`My Own Server / Home Relay Server` means a relay owned and controlled by the user, such as an always-on PC, Raspberry Pi, NAS, Linux host, Docker host, Home Assistant system, or similar home-side device.

## Components

### Android App

The Android app is the main user-facing PowerBridge client.

It can:

* store PC profiles
* import setup data from QR
* send local Wake-on-LAN packets
* show wake-method readiness
* run guided setup
* generate diagnostics when the user requests them
* share diagnostics through the Android native share sheet

See [android/README.md](android/README.md).

### Windows Companion

The Windows Companion is a setup helper.

It can:

* detect local adapter/network values
* prepare setup data for the Android app
* generate scan-safe QR setup payloads
* help the user avoid manual network typing

It does not:

* wake the PC directly
* install a wake service
* modify BIOS or UEFI settings
* modify router settings
* modify firewall settings
* collect secrets
* run as a remote relay

See [windows-companion/README.md](windows-companion/README.md).

## Runtime Truth

Live now:

* `Home Wi-Fi only` → `Local Wi-Fi Wake`
* `My Own Server` → `Home Relay Server` for advanced user-owned relay setups

Prototype / next target:

* `Old Android Device`

Coming later / research:

* `Alexa / Google Voice Devices`
* Firebase / FCM
* cloud broker
* smart-home runtime
* remote request delivery for old Android relay

Removed from active scope:

* TV / streaming relay
* Fire TV / Android TV relay
* Roku relay
* smart plug / hardware boot
* router / VPN as a standalone method

## Honest Technical Limits

Wake-on-LAN depends on hardware, firmware, power state, driver behavior, operating-system configuration, and network topology.

Important limits:

* Ethernet is recommended for the most reliable Wake-on-LAN behavior.
* Wi-Fi wake may work on some laptops and devices, but it is hardware-dependent.
* Boot from full shutdown is often less reliable than wake from sleep.
* Remote or cellular wake requires a valid user-owned home-side path.
* PowerBridge does not bypass NAT, firewalls, OS permissions, hardware limits, paid services, or vendor restrictions.
* PowerBridge can send wake attempts, but it cannot guarantee every PC or network will respond.

## Privacy Stance

PowerBridge does not include analytics by default.

Diagnostics are generated locally and shared only when the user explicitly chooses to share them. Generated QR payloads, setup JSON files, logs, screenshots, and diagnostics may contain local network values and should not be posted publicly without redaction.

Do not publish:

* generated diagnostics ZIPs
* generated QR screenshots with real local values
* generated setup JSON payloads with real local values
* logs containing local network details
* screenshots showing real IP addresses, MAC addresses, tokens, or endpoints
* signing keys
* keystores
* signing properties
* `local.properties`

See:

* [docs/PRIVACY.md](docs/PRIVACY.md)
* [docs/PUBLIC-SANITIZATION.md](docs/PUBLIC-SANITIZATION.md)

## Build Overview

Android debug build:

```powershell
Set-Location "<repo-root>\android"
.\gradlew.bat :app:assembleDebug --no-daemon
```

Android release validation build:

```powershell
Set-Location "<repo-root>\android"
.\gradlew.bat :app:assembleRelease --no-daemon
```

Release-signing truth:

* `assembleDebug` produces a development APK.
* Debug APKs are not production-signed.
* `assembleRelease` may be unsigned in public CI when local signing properties are absent.
* Unsigned release APKs are acceptable for CI validation but not for official public Android release assets.
* Official public Android release assets should come from a locally signed release build.
* GitHub Actions currently validates Android builds but does not hold private signing secrets in this phase.

Windows Companion validation:

```powershell
Set-Location "<repo-root>\windows-companion"
pwsh -NoProfile -ExecutionPolicy Bypass -File ".\scripts\Test-PowerBridgeCompanion.ps1"
```

Windows Companion CLI detection without clipboard dependency:

```powershell
Set-Location "<repo-root>\windows-companion"
pwsh -NoProfile -ExecutionPolicy Bypass -File ".\src\PowerBridgeCompanion.ps1" -NoClipboard
```

More detail is in [BUILD.md](BUILD.md).

## Release Layout

When public releases begin, the GitHub release page should expose deliverables as separate top-level assets, not as a bundled folder or nested archive.

Target release asset shape:

* `PowerBridge-vX.Y.Z.apk`
* `PowerBridge-Companion-Setup-vX.Y.Z.exe`

Release rules:

* keep the Android APK and Windows Companion installer as separate assets
* do not wrap them in an extra ZIP just to group them
* let users download the Android app or Windows installer independently from the same release page
* do not publish debug or unsigned Android artifacts as official public release assets
* do not publish `v1.0.0` until all selected final methods are working, tested, documented, packaged, and consumer-ready

Current local release flow:

```powershell
Set-Location "<repo-root>"
pwsh -NoProfile -ExecutionPolicy Bypass -File ".\scripts\Build-PowerBridgeReleaseAssets.ps1"
pwsh -NoProfile -ExecutionPolicy Bypass -File ".\scripts\Publish-PowerBridgeRelease.ps1"
```

## Important Notice

PowerBridge is independent software provided without warranty. Use it at your own risk.

PowerBridge does not:

* guarantee wake success from every power state
* guarantee wake over every Wi-Fi, router, ISP, or cellular path
* bypass authentication or security controls
* modify router, BIOS, firewall, or operating-system settings automatically
* provide a shared public relay service
* provide shared wake tokens
* provide developer-managed private infrastructure

See [INSTALL-NOTICE.txt](INSTALL-NOTICE.txt).

## Release Safety

Before publishing a public release:

* run Android build validation
* run Windows Companion validation
* verify release signing state
* verify release asset names
* verify checksums
* run a public-value sweep
* confirm GitHub Actions are green
* confirm no generated diagnostics, QR payloads, logs, screenshots, signing material, or local values are staged
* confirm `INSTALL-NOTICE.txt` is current
* confirm the README does not claim unfinished methods are ready

## License

PowerBridge source and project-owned documentation in this repository are released under the MIT License. Third-party dependencies keep their own licenses.

See [LICENSE](LICENSE) and [THIRD-PARTY-NOTICES.md](THIRD-PARTY-NOTICES.md).
