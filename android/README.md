# PowerBridge

PowerBridge is a public Android wake client for saved PC profiles. It helps users wake or boot a PC using Wake-on-LAN when the phone and PC are on the same local network, or through an advanced user-owned relay path.

Current public version truth:

```text
v0.6.1
```

Current development version truth:

```text
0.7.6
```

Current phase truth:

* `Phase 12` is complete as architecture
* `Phase 13` is complete as contract/planning work
* `Phase 13.5` is complete as cleanup/readiness work
* `Phase 14A` is complete as the AIO relay-mode correction pass
* `Phase 14B` is complete as guided wake setup and method selection
* `Phase 14C` is complete as wake-path readiness wiring and honest setup-state labeling
* `Phase 14C.1` is complete as guided setup polish, Home Relay report sharing, CI filter cleanup, and version alignment
* `Phase 14D` is complete as main-APK method guide and readiness realignment
* `Phase 14E` is next for one selected method-specific runtime prototype

PowerBridge v1 is intentionally focused on wake and boot. Shutdown, restart, hibernate, remote desktop control, and target-side command execution are out of scope because they require a separate backend, always-on agent, operating-system access, or device-specific integration.

## What PowerBridge Does

PowerBridge is designed around saved PC profiles. Each profile can store the setup needed to wake a specific PC.

Current core features:

* Save multiple PC profiles
* Wake a PC from sleep using Wake-on-LAN
* Attempt boot from shutdown when hardware supports it
* Import setup data from PowerBridge Windows Companion by QR code
* Check local setup readiness
* Export diagnostics as a ZIP report
* Support local Wake-on-LAN and advanced user-owned relay setups
* Expose an AIO `Home Relay Mode` prototype inside the same Android app
* Guide users through plain-language wake method selection from the main app

## Privacy and Public-Safety Boundary

PowerBridge is public, generic, and user-configured.

PowerBridge must not ship with developer-specific infrastructure, private relay assumptions, private domains, private IP addresses, private MAC addresses, private tokens, or personal setup values.

`Home Relay Server` always means a relay owned and controlled by the user, such as a Raspberry Pi, NAS, server, router, Docker host, or personal domain relay.

PowerBridge does not upload diagnostics automatically. Diagnostic reports are generated locally and shared only when the user chooses to share them.

## Build and Install

Build from the Android project root:

```powershell
Set-Location "<repo-root>\android"
.\gradlew.bat assembleDebug --no-daemon
.\gradlew.bat assembleRelease --no-daemon
```

Debug APK output:

```text
android\app\build\outputs\apk\debug\app-debug.apk
```

If you want to install the debug APK over USB from PowerShell:

```powershell
& "<android-sdk>\platform-tools\adb.exe" install -r ".\app\build\outputs\apk\debug\app-debug.apk"
```

Release signing must use a private keystore that is not committed to the repository.

Current release-signing foundation:

* debug APKs remain the easiest local install/test artifact
* `assembleRelease` can run in CI or locally without signing properties
* a locally signed release APK is only produced when ignored signing properties are configured
* GitHub Actions validation builds are not the same thing as an official signed public Android release build

Keep Android signing material and private keystore paths outside the repository.

## What Works Now

| Method              | What it does                                                                            | Best for                                              | Hardware needed                                                  | Cellular support                    | Status |
| ------------------- | --------------------------------------------------------------------------------------- | ----------------------------------------------------- | ---------------------------------------------------------------- | ----------------------------------- | ------ |
| `Local Wi-Fi Wake`  | Sends Wake-on-LAN Magic Packets directly from the Android phone over the local network  | Most users at home on the same Wi-Fi or LAN as the PC | Android phone, PC Wake-on-LAN support, same network              | No                                  | Live   |
| `Home Relay Server` | Sends a secure request to a user-owned relay that performs the local Wake-on-LAN action | Advanced self-hosted users                            | Raspberry Pi, NAS, server, router, Docker host, or similar relay | Yes, with user-owned infrastructure | Live   |

## Planned Methods

These methods are planned or under research. They are not live wake engines yet.

| Method                   | Intended shape                                                                                                      | Current status |
| ------------------------ | ------------------------------------------------------------------------------------------------------------------- | -------------- |
| `Old Phone / Tablet Relay` | Use a spare Android phone or tablet left plugged in at home as the future local wake anchor                       | Prototype      |
| `Fire TV / Smart TV Relay` | Use a TV-class Android device left powered at home as the future wake helper                                      | Coming later   |
| `Smart Plug Power-On`      | Power-cycle a smart plug and rely on BIOS or UEFI restore-after-power-loss behavior to boot the PC                | Coming later   |
| `Smart Speaker Wake`       | Integrate with smart-home ecosystems such as Alexa, Google Home, or Home Assistant where platform support allows it | Research     |
| `Advanced Network Setup`   | Provide router, VPN, NAS, server, and custom-network guidance                                                     | Guides later   |

## Phase 14E Direction

Phase 14D completed the method-clarity and readiness pass. Phase 14E should choose one method-specific runtime prototype instead of trying to implement every ecosystem at once:

* old phone or tablet relay runtime
* Fire TV / Smart TV relay runtime
* smart plug integration prototype
* smart speaker integration research spike
* persistent server setup helper

Phase 14E should not start all of these at once. Pick one path, wire it honestly, test it, then move to the next.

See [../docs/WAKE-METHODS.md](../docs/WAKE-METHODS.md) for the current method truth.

## Recommended Setup Paths

| Situation                                                          | Recommended path                        |
| ------------------------------------------------------------------ | --------------------------------------- |
| Phone and PC are on the same Wi-Fi or LAN                          | `Local Wi-Fi Wake`                      |
| User wants fast setup for the current Windows PC                   | PowerBridge Windows Companion QR import |
| User already has a home server, NAS, Raspberry Pi, or router relay | `Home Relay Server`                     |
| User wants remote wake over cellular without a server              | Future `Home Relay Mode` path after prototype work |
| PC does not wake reliably through normal Wake-on-LAN               | Future `Smart Plug Boot Assist`         |

## Honest Technical Limits

Wake-on-LAN has real network boundaries.

A public cloud server cannot directly send a Wake-on-LAN UDP Magic Packet into a private home LAN unless something inside that network can receive a request and send the local packet. That local path may be a user-owned relay, an always-on home device, a VPN/router setup, or a supported smart-home/hardware path.

A sleeping or powered-off PC also cannot wake itself through a normal operating-system agent because the OS and normal network stack are offline.

Other important limits:

* `Windows Companion` is a setup helper. It does not wake the PC directly.
* Ethernet is recommended for the most reliable Wake-on-LAN behavior.
* Wi-Fi wake may work on some laptops and devices, but it depends on hardware, firmware, drivers, power state, and operating-system behavior.
* Boot from full shutdown is often less reliable than wake from sleep because BIOS/UEFI, NIC firmware, motherboard support, and Windows Fast Startup settings can affect the result.
* Remote wake over cellular requires a valid home-side path. PowerBridge does not bypass NAT or firewall limitations by itself.

## Windows Companion

PowerBridge Windows Companion is a Windows-side setup helper. It detects local PC/network details and generates a scan-safe QR code that the Android app can import.

It detects:

* PC name
* Adapter type
* MAC address
* IPv4 address
* Gateway
* Subnet prefix
* Broadcast IP

It generates:

* `powerbridge.local_setup.v1` setup payload
* Scan-safe black-on-white QR code
* Local setup data for a `Local Wi-Fi Wake` profile

It does not:

* Wake the PC directly
* Run in the background
* Install a Windows service
* Change firewall, power, BIOS, or NIC settings
* Collect passwords, credentials, relay tokens, or secrets
* Solve remote cellular wake by itself

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
7. If the user chooses `Away from home`, PowerBridge shows these five remote families:
   * `Old Phone / Tablet Relay`
   * `Fire TV / Smart TV Relay`
   * `Smart Speaker Wake`
   * `Smart Plug Power-On`
   * `My Own Server`
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

* Profile name
* Wake method
* Target MAC address
* Target IP address
* Broadcast IP
* Gateway/subnet details
* Relay settings when using `Home Relay Server`

Supported profile actions:

* Add profile
* Rename profile
* Switch active profile
* Delete profile
* Import setup by QR
* Update an existing profile from QR

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

### Home Relay Server

Use this only if you already have, or are comfortable running, a user-owned relay.

Examples:

* Raspberry Pi
* NAS
* Linux server
* Docker host
* Router-based relay
* Personal domain relay

Security guidance:

* Keep relay tokens private.
* Use HTTPS where possible.
* Do not expose raw Wake-on-LAN UDP ports directly to the public internet.
* Do not publish private relay URLs, tokens, MAC addresses, or IP addresses.

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

* Import is previewed before saving.
* User chooses `Create New Profile` or `Update Current Profile`.
* Imported Windows Companion profiles use `Local Wi-Fi Wake`.
* Import never accepts relay tokens, credentials, passwords, API keys, or secrets.
* Imported MAC addresses are masked in diagnostics.
* Diagnostics ZIP sharing remains user-controlled.

## Diagnostics

PowerBridge can generate a full diagnostics ZIP when the user explicitly chooses to share it.

Rules:

* diagnostics are not auto-uploaded
* diagnostics may contain local environment details
* public bug reports should be redacted before posting
* generated diagnostics ZIPs must not be committed to source control

## Deployment Categories for Later Phases

These categories guide future development. They are not all implemented today.

| Category            | Examples                                                          | Intended role                                     |
| ------------------- | ----------------------------------------------------------------- | ------------------------------------------------- |
| `Legacy Mobile`     | Old Android phones, Pixel devices, Galaxy devices, Fire tablets   | Always-on home relay device                       |
| `Media Client`      | Fire TV, Chromecast with Google TV, Android TV, Google TV         | Always-on or semi-awake home network anchor       |
| `Voice Ecosystem`   | Alexa, Google Home, Home Assistant                                | Platform-dependent smart-home wake integration    |
| `Hardware Bypass`   | Smart plugs, PC power switch hardware                             | Boot assist for systems that do not wake normally |
| `Persistent Server` | Raspberry Pi, Synology NAS, TrueNAS, Unraid, Docker, Linux server | Advanced user-owned relay path                    |

## Condensed Roadmap

| Phase      | Name                             | Purpose                                                        |
| ---------- | -------------------------------- | -------------------------------------------------------------- |
| `Phase 10` | Setup + Method UX Cleanup        | Clean method names, setup wording, warnings, and public docs   |
| `Phase 11` | Android Release Signing Foundation | Formalize safe local Android release signing                   |
| `Phase 12` | Home Device Relay Architecture   | Design the old-phone/tablet/TV relay path                      |
| `Phase 13` | Prototype Planning + Contract Validation | Define relay contracts and the controlled prototype plan       |
| `Phase 13.5` | Pre-Prototype Cleanup + Readiness Audit | Tighten docs, examples, and repo hygiene before runtime work |
| `Phase 14A` | Home Relay Mode AIO Correction  | Move relay prototype direction into the main Android app      |
| `Phase 14B` | Guided Wake Setup Framework     | Add plain-language wake setup and method selection guidance   |
| `Phase 14C` | Wake Path Readiness + Honest Wiring | Make guided setup honest about what is ready now, what needs setup, and what is still planned |
| `Phase 14D` | Main APK Method Guides + Readiness Follow-Through | Improve user-facing method guidance and readiness clarity before deeper relay runtime |
| `Phase 15` | Alternate Wake Paths + Guides    | Smart plug, smart-home, NAS, router, and advanced setup guides |
| `Phase 16` | Project Packaging + Release Prep | Repo structure, shared contracts, release packaging, checksums |

## Guardrails

PowerBridge v1 will not include:

* Shutdown
* Restart
* Hibernate
* Remote desktop control
* Target-side command execution
* Windows credential storage
* Automatic firewall changes
* Automatic router configuration
* Raw secret import/export
* Auto-uploaded diagnostics
* Analytics by default

PowerBridge should remain:

* Generic
* Configurable
* Public-safe
* Diagnostics-heavy
* Simple-user friendly
* Honest about network limitations

## Related Docs

* [../README.md](../README.md)
* [../BUILD.md](../BUILD.md)
* [../docs/PRIVACY.md](../docs/PRIVACY.md)
* [docs/PHASE-PLAN.md](docs/PHASE-PLAN.md)
