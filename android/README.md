# PowerBridge Android

PowerBridge Android is the main Android client for PowerBridge.

It helps users save PC profiles and send Wake-on-LAN wake/boot attempts when the phone and PC are on the same local network, or when the user has configured an approved user-owned relay path.

Current public version:

```text
v0.6.1
```

Current development version:

```text
0.7.7
```

## Current Product Truth

PowerBridge v1 is focused on wake and boot attempts.

PowerBridge does not support:

* shutdown
* restart
* hibernate
* remote desktop control
* target-side command execution
* Windows credential storage
* automatic firewall changes
* automatic router configuration
* raw secret import/export
* auto-uploaded diagnostics
* analytics by default

These features are out of scope because they require a separate backend, always-on target-side agent, operating-system access, device-specific integration, or a different security model.

## Current Phase Truth

Completed:

* `Phase 12` — architecture
* `Phase 13` — contract and prototype planning
* `Phase 13.5` — cleanup and readiness work
* `Phase 14A` — AIO relay-mode correction pass
* `Phase 14B` — guided wake setup and method selection
* `Phase 14C` — wake-path readiness wiring and honest setup-state labeling
* `Phase 14C.1` — guided setup polish, Home Relay report sharing, CI filter cleanup, and version alignment
* `Phase 14D` — main-APK method guide and readiness realignment
* `Phase 14E` — three-method scope reduction and Old Android preparation pass

Next runtime direction:

* `Phase 14F` — Old Android relay runtime foundation

## What PowerBridge Does

PowerBridge is designed around saved PC profiles. Each profile stores the setup needed to wake a specific PC.

Current core features:

* save multiple PC profiles
* wake a PC from sleep using Wake-on-LAN
* attempt boot from shutdown when hardware supports it
* import setup data from PowerBridge Windows Companion by QR code
* check local setup readiness
* export diagnostics as a ZIP report when the user chooses
* support local Wake-on-LAN
* support advanced user-owned relay setups through `Home Relay Server`
* guide users through plain-language wake method selection

## Privacy and Public-Safety Boundary

PowerBridge is public, generic, and user-configured.

PowerBridge must not ship with:

* developer-specific infrastructure
* private relay assumptions
* private domains
* private IP addresses
* private MAC addresses
* private tokens
* personal setup values
* shared public relay credentials

`Home Relay Server` always means a relay owned and controlled by the user, such as a Raspberry Pi, NAS, server, Docker host, always-on PC, or similar home-side system.

PowerBridge does not upload diagnostics automatically. Diagnostic reports are generated locally and shared only when the user chooses to share them.

## Build and Install

Build from the Android project root:

```powershell
Set-Location "<repo-root>\android"
.\gradlew.bat :app:assembleDebug --no-daemon
.\gradlew.bat :app:assembleRelease --no-daemon
```

Debug APK output:

```text
android\app\build\outputs\apk\debug\app-debug.apk
```

Install the debug APK over USB from PowerShell:

```powershell
Set-Location "<repo-root>\android"
& "<android-sdk>\platform-tools\adb.exe" install -r ".\app\build\outputs\apk\debug\app-debug.apk"
```

## Release Signing Truth

Android release signing must use a private keystore that is not committed to the repository.

Current release-signing foundation:

* debug APKs remain the easiest local install/test artifact
* `assembleRelease` can run in CI or locally without signing properties
* a locally signed release APK is produced only when ignored signing properties are configured
* GitHub Actions validation builds are not official signed public Android release builds
* official public Android release APK assets should come from a locally signed release build

Keep Android signing material, signing properties, passwords, and private keystore paths outside the repository.

## What Works Now

| Method                                | What it does                                                                            | Best for                                              | Hardware needed                                                        | Cellular support                    | Status          |
| ------------------------------------- | --------------------------------------------------------------------------------------- | ----------------------------------------------------- | ---------------------------------------------------------------------- | ----------------------------------- | --------------- |
| `Local Wi-Fi Wake`                    | Sends Wake-on-LAN Magic Packets directly from the Android phone over the local network  | Most users at home on the same Wi-Fi or LAN as the PC | Android phone, PC Wake-on-LAN support, same network                    | No                                  | Live            |
| `Home Relay Server` / `My Own Server` | Sends a secure request to a user-owned relay that performs the local Wake-on-LAN action | Advanced self-hosted users                            | Raspberry Pi, NAS, server, Docker host, always-on PC, or similar relay | Yes, with user-owned infrastructure | Live / advanced |

## Approved Remote Model

PowerBridge’s approved remote wake model is limited to:

| Method                                | Intended shape                                                                              | Current status          |
| ------------------------------------- | ------------------------------------------------------------------------------------------- | ----------------------- |
| `Old Android Device`                  | Use a spare Android phone or tablet left plugged in at home as the future local wake anchor | Next runtime prototype  |
| `My Own Server` / `Home Relay Server` | Use a user-owned always-on NAS, Raspberry Pi, Docker host, server, or PC                    | Live for advanced users |
| `Alexa / Google Voice Devices`        | Integrate with supported voice ecosystems where a safe wake path is possible                | Research / later        |

Removed from active scope:

* TV / streaming relay
* Fire TV / Android TV relay
* Roku relay
* smart plug / hardware power-cycle boot
* router / VPN as a standalone method
* shutdown / restart workflows

Next runtime work should start with `Old Android Device`, because it is the most user-friendly remote-wake path if it can be made reliable.

See [../docs/WAKE-METHODS.md](../docs/WAKE-METHODS.md) for current method truth.

## Recommended Setup Paths

| Situation                                                                       | Recommended path                        |
| ------------------------------------------------------------------------------- | --------------------------------------- |
| Phone and PC are on the same Wi-Fi or LAN                                       | `Local Wi-Fi Wake`                      |
| User wants fast setup for the current Windows PC                                | PowerBridge Windows Companion QR import |
| User already has a home server, NAS, Raspberry Pi, Docker host, or always-on PC | `Home Relay Server` / `My Own Server`   |
| User wants remote wake over cellular without a server                           | Future `Old Android Device` relay path  |

## Honest Technical Limits

Wake-on-LAN has real network boundaries.

A public cloud service cannot directly send a Wake-on-LAN UDP Magic Packet into a private home LAN unless something inside that network can receive a request and send the final local packet. That local path may be a user-owned relay, an always-on home device, or a supported smart-home path.

A sleeping or powered-off PC also cannot wake itself through a normal operating-system agent because the OS and normal network stack are offline.

Important limits:

* Windows Companion is a setup helper. It does not wake the PC directly.
* Ethernet is recommended for the most reliable Wake-on-LAN behavior.
* Wi-Fi wake may work on some laptops and devices, but it depends on hardware, firmware, drivers, power state, and operating-system behavior.
* Boot from full shutdown is often less reliable than wake from sleep because BIOS/UEFI, NIC firmware, motherboard support, Windows Fast Startup, and standby power behavior can affect the result.
* Remote wake over cellular requires a valid home-side path.
* PowerBridge does not bypass NAT, firewalls, OS permissions, hardware limits, paid services, or vendor restrictions.

## Windows Companion

PowerBridge Windows Companion is a Windows-side setup helper. It detects local PC/network details and generates a scan-safe QR code that the Android app can import.

It detects:

* PC name
* adapter type
* MAC address
* IPv4 address
* gateway
* subnet prefix
* broadcast IP

It generates:

* `powerbridge.local_setup.v1` setup payload
* scan-safe black-on-white QR code
* local setup data for a `Local Wi-Fi Wake` profile

It does not:

* wake the PC directly
* run in the background
* install a Windows service
* change firewall, power, BIOS, UEFI, NIC, or router settings
* collect passwords, credentials, relay tokens, API keys, or secrets
* solve remote cellular wake by itself

## Current User Flow

### First-Run Guided Setup

1. Open PowerBridge.
2. Tap `Setup Guide`.
3. Scan the QR from PowerBridge Windows Companion.
4. Review the detected PC and keep or rename the profile.
5. Confirm the PC.
6. Choose where wake should work:

   * `Home Wi-Fi only`
   * `Away from home`
   * `Not sure`
7. If the user chooses `Away from home`, PowerBridge shows the approved remote families:

   * `Old Android Device`
   * `My Own Server`
   * `Alexa / Google Voice Devices`
8. PowerBridge shows an honest readiness state:

   * `Ready now`
   * `Needs setup`
   * `Prototype`
   * `Coming later`
   * `Advanced`
9. The final setup-status card explains what to do next.

### Windows

1. Open PowerBridge Windows Companion.
2. Select the adapter you want to pair.
3. Click `Detect This PC`.
4. Verify the detected values.
5. Click `Generate QR Code`.

### Android

1. Open PowerBridge.
2. Go to `Settings -> PC Profiles`.
3. Tap `Scan Setup QR`.
4. Scan the QR from Windows Companion.
5. Review the detected setup.
6. Choose `Create New Profile` or `Update Current Profile`.
7. Return to the main screen.
8. Confirm the correct `Active PC`.
9. Run `Check Local Setup`.
10. Use `Wake PC`, `Boot PC`, or `Open Diagnostics`.

## PC Profiles

PowerBridge supports multiple saved PC profiles.

Profiles can store separate wake settings for different PCs, including:

* profile name
* wake method
* target MAC address
* target IP address
* broadcast IP
* gateway/subnet details
* relay settings when using `Home Relay Server`

Supported profile actions:

* add profile
* rename profile
* switch active profile
* delete profile
* import setup by QR
* update an existing profile from QR

PowerBridge keeps at least one profile available and blocks deleting the final profile.

## Setup Guidance

### Local Wi-Fi Wake

Use this when the Android phone and PC are on the same home network.

Recommended steps:

1. Use PowerBridge Windows Companion to generate a setup QR.
2. Scan the QR in Android with `Settings -> PC Profiles -> Scan Setup QR`.
3. Confirm the imported profile uses `Local Wi-Fi Wake`.
4. Keep the phone on the same Wi-Fi or LAN as the PC.
5. Prefer Ethernet on the PC for best reliability.
6. Enable Wake-on-LAN support in BIOS/UEFI and Windows/NIC settings where required.

### Home Relay Server / My Own Server

Use this only if you already have, or are comfortable running, a user-owned relay.

Examples:

* Raspberry Pi
* NAS
* Linux server
* Docker host
* always-on PC
* personal home relay server

Security guidance:

* keep relay tokens private
* use HTTPS where possible
* do not expose raw Wake-on-LAN UDP ports directly to the public internet
* do not publish private relay URLs, tokens, MAC addresses, or IP addresses
* do not use shared public relay credentials

### Old Android Device

This is the next planned runtime direction.

Intended shape:

* old Android phone or tablet stays at home
* device remains plugged in and connected to home Wi-Fi
* device acts as the local home-side wake sender
* main phone eventually triggers the relay through a safe user-owned request path

Current status:

* not live as a full remote wake engine yet
* next runtime prototype target

## Import Contract

PowerBridge Android imports setup data using this schema:

```text
powerbridge.local_setup.v1
```

Entry point:

```text
Settings -> PC Profiles -> Scan Setup QR
```

Import behavior:

* import is previewed before saving
* user chooses `Create New Profile` or `Update Current Profile`
* imported Windows Companion profiles use `Local Wi-Fi Wake`
* import never accepts relay tokens, credentials, passwords, API keys, or secrets
* imported MAC addresses are masked in diagnostics
* diagnostics ZIP sharing remains user-controlled

## Diagnostics

PowerBridge can generate a full diagnostics ZIP when the user explicitly chooses to share it.

Rules:

* diagnostics are not auto-uploaded
* diagnostics may contain local environment details
* public bug reports should be redacted before posting
* generated diagnostics ZIPs must not be committed to source control
* screenshots showing real setup values should not be posted publicly without redaction

## Current Condensed Roadmap

| Phase         | Name                                 | Purpose                                                                     |
| ------------- | ------------------------------------ | --------------------------------------------------------------------------- |
| `Phase 14B`   | Guided Setup Foundation              | Guided wake setup and plain-language method selection                       |
| `Phase 14C`   | Honest Wake-Path Readiness           | Readiness wiring for `Home Wi-Fi only`, `Away from home`, and `Not sure`    |
| `Phase 14C.1` | Guided Setup Polish                  | Completion flow, Home Relay report sharing, CI filters, version alignment   |
| `Phase 14D`   | Main APK Method Realignment          | Method guide and readiness cleanup                                          |
| `Phase 14E`   | Approved Remote Model Reset          | Strip unreliable method families and lock approved remote paths             |
| `Phase 14F`   | Old Android Relay Runtime Foundation | Begin Old Android Device relay runtime foundation                           |
| `Phase 15`    | Alternate Wake Paths and Guides      | Deeper runtime work and selected future method guidance                     |
| `Phase 16`    | Packaging and Release Prep           | Signing, packaging, release assets, checksums, and public release readiness |

Current roadmap truth:

* `Phase 14E` is complete.
* `Phase 14F` is the next runtime foundation phase.
* The next runtime target is `Old Android Device`.
* `v1.0.0` should not ship until all selected final methods are working, tested, documented, packaged, and consumer-ready.

## Guardrails

PowerBridge should remain:

* generic
* configurable
* public-safe
* diagnostics-heavy
* simple-user friendly
* honest about network limitations

PowerBridge Android must not silently add:

* shutdown
* restart
* hibernate
* remote desktop control
* target-side command execution
* Windows credential storage
* automatic firewall changes
* automatic router configuration
* raw secret import/export
* auto-uploaded diagnostics
* analytics by default
* hidden developer relay defaults
* shared public relay credentials

## Related Docs

* [../README.md](../README.md)
* [../BUILD.md](../BUILD.md)
* [../INSTALL-NOTICE.txt](../INSTALL-NOTICE.txt)
* [../docs/PRIVACY.md](../docs/PRIVACY.md)
* [../docs/WAKE-METHODS.md](../docs/WAKE-METHODS.md)
* [docs/PHASE-PLAN.md](docs/PHASE-PLAN.md)
