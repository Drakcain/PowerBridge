# PowerBridge Phase Plan

PowerBridge is a public Android wake client with saved PC profiles, a Windows setup companion, local Wake-on-LAN support, and an advanced user-owned relay path.

This phase plan tracks what works today, what is planned, and what must remain out of scope for a safe public release.

## Current Product Truth

PowerBridge currently supports:

* Saved PC profiles
* `Local Wi-Fi Wake`
* `Home Relay Server`
* Windows Companion QR setup
* Reviewed QR import on Android
* Profile-based diagnostics
* Full diagnostics ZIP sharing

PowerBridge does not currently support:

* `Home Device Relay`
* `Smart Plug Boot Assist`
* `Smart Home Wake`
* `Advanced Network Setup` guides
* Shutdown
* Restart
* Hibernate
* Target-side remote control
* Windows background agent control

## Hard Wake-on-LAN Boundary

Wake-on-LAN has a real networking boundary.

A phone on cellular cannot directly send a Wake-on-LAN Magic Packet into a private home LAN unless something inside that home environment can receive a request and send the local packet.

That local path may be:

* A phone on the same Wi-Fi or LAN
* A user-owned relay server
* An always-on home device
* A router or VPN path
* A smart plug or hardware bypass
* A supported smart-home integration

A sleeping or powered-off PC also cannot wake itself through a normal OS-side agent because the operating system and normal network stack are not running.

PowerBridge must keep this boundary clear in UI, docs, diagnostics, and setup guidance.

## Public Method Taxonomy

| Method                   | Purpose                                                                   | Intended user                                                                  | Current status |
| ------------------------ | ------------------------------------------------------------------------- | ------------------------------------------------------------------------------ | -------------- |
| `Local Wi-Fi Wake`       | Direct same-LAN Wake-on-LAN from Android                                  | Most users at home on the same network as the PC                               | Live           |
| `Home Relay Server`      | Advanced self-hosted relay path                                           | Users with a Raspberry Pi, NAS, server, router, Docker host, or similar relay  | Live           |
| `Home Device Relay`      | Always-on Android, tablet, Fire TV, Android TV, or Google TV home anchor  | Users who want remote wake without a full server                               | Coming later   |
| `Smart Plug Boot Assist` | Hardware power-cycle workaround using smart plug or power-switch hardware | Users with PCs that do not wake reliably through normal WOL                    | Coming later   |
| `Smart Home Wake`        | Voice or smart-home ecosystem integration                                 | Users with supported Alexa, Google Home, Home Assistant, or similar ecosystems | Research       |
| `Advanced Network Setup` | Router WOL, VPN, NAS, server, and custom-network guidance                 | Power users                                                                    | Guides later   |

## Deployment Categories to Preserve

These categories guide future planning. They should not be presented as live features until implemented.

| Category            | Examples                                                  | Intended role                           |
| ------------------- | --------------------------------------------------------- | --------------------------------------- |
| `Legacy Mobile`     | Old Android phones, tablets, Fire tablets                 | Always-on home relay device             |
| `Media Client`      | Fire TV, Chromecast with Google TV, Android TV, Google TV | Home network anchor                     |
| `Voice Ecosystem`   | Alexa, Google Home, Home Assistant                        | Platform-dependent smart-home wake path |
| `Hardware Bypass`   | Smart plugs, power-switch cards                           | Boot assist for difficult hardware      |
| `Persistent Server` | Raspberry Pi, NAS, Docker host, Linux server              | Advanced user-owned relay path          |

## Completed Phases

### Phase 1 — WakeMethod Foundation

Established the method-based architecture that lets PowerBridge support multiple wake paths without hardcoding one private setup.

### Phase 2 — Real Local Wi-Fi Wake

Implemented direct local Wake-on-LAN from Android using UDP Magic Packets.

### Phase 3 — Local Setup Assist

Added Android-side local network detection and setup guidance for broadcast, gateway, and local readiness.

### Phase 4 — Public Setup Polish and Guardrails

Cleaned setup wording, made unsupported methods safer, and added public-safe guardrails.

### Phase 5 — Local-First Defaults and Placeholder Guardrails

Made `Local Wi-Fi Wake` the default path and treated placeholder values as setup-needed instead of real configuration.

### Phase 6 — PC Profiles Foundation

Added per-PC profiles with active profile switching, profile-specific settings, diagnostics integration, and main-screen profile awareness.

### Phase 7 — Android QR Import Contract

Added Android-side import support for the `powerbridge.local_setup.v1` setup contract.

### Phase 8 — Windows Companion

Built the Windows Companion setup helper.

Current Windows Companion behavior:

* User selects an adapter
* Companion detects PC/network setup
* Companion generates scan-safe QR
* Android scans QR and previews the setup
* User creates or updates a PC profile

### Phase 9 — AIO Pairing Polish

Polished the full Windows-to-Android setup flow.

Current pairing flow:

1. Open Windows Companion.
2. Select adapter.
3. Click `Detect This PC`.
4. Click `Generate QR Code`.
5. Open PowerBridge on Android.
6. Go to `Settings -> PC Profiles -> Scan Setup QR`.
7. Scan the QR.
8. Review the import preview.
9. Choose `Create New Profile` or `Update Current Profile`.
10. Confirm `Active PC`.
11. Run `Check Local Setup`.
12. Use `Diagnostics` or `Share Full Report` if needed.

### Phase 10 — Setup + Method UX Cleanup

Cleaned public method names, status labels, setup wording, warnings, docs, and roadmap structure.

Phase 10 keeps only the implemented methods live:

* `Local Wi-Fi Wake`
* `Home Relay Server`

All future methods remain clearly marked as coming later, guides later, or research.

## Current Supported Flow

### Local Wi-Fi Wake Setup

1. Open Windows Companion.
2. Select the adapter used by the PC.
3. Click `Detect This PC`.
4. Click `Generate QR Code`.
5. Scan the QR in Android.
6. Review the imported setup.
7. Create or update a profile.
8. Confirm the profile uses `Local Wi-Fi Wake`.
9. Run `Check Local Setup`.
10. Use `Wake PC` or `Boot PC`.

### Home Relay Server Setup

`Home Relay Server` is for advanced users with their own relay infrastructure.

The user must provide and secure their own:

* Relay host
* Relay URL
* Token
* Home-side WOL path
* Network/routing setup

PowerBridge should never instruct users to expose raw Wake-on-LAN UDP ports directly to the public internet.

## Condensed Roadmap

### Phase 11 — Home Device Relay Architecture

Design the secure always-on home-anchor model.

Planned focus:

* Old Android phone relay
* Android tablet relay
* Fire TV / Android TV / Google TV feasibility
* Secure pairing model
* Cloud-event boundary
* Local WOL sender behavior
* Diagnostics and trust model

No implementation should begin until the architecture is clear.

### Phase 12 — Home Device Relay Prototype

Build the first working home-device relay prototype.

Planned focus:

* Relay device app/mode
* Secure registration/pairing
* Receiving wake requests
* Sending local WOL from inside the home network
* Diagnostics for relay online/offline state
* Battery and background behavior guidance

### Phase 13 — Alternate Wake Paths + Guides

Document and research secondary wake paths.

Planned focus:

* Smart plug boot assist
* BIOS/UEFI restore-after-power-loss guidance
* Router/VPN/NAS/server guides
* Smart-home ecosystem feasibility
* Hardware bypass warnings
* Platform limitations

### Phase 14 — Project Packaging + Release Prep

Prepare the project for public release.

Planned focus:

* Public repo hygiene
* Release packaging
* Android APK packaging
* Windows Companion packaging
* Shared contract docs
* Checksums
* Versioning
* Final privacy and private-value sweeps
* Public documentation cleanup

## Rules for Future Phases

PowerBridge must remain:

* Public
* Generic
* User-configured
* Honest about limitations
* Free of developer-specific private values
* Safe for public release

Future work must not:

* Claim unimplemented methods are live
* Overpromise cellular wake
* Overpromise Wi-Fi wake reliability
* Hide the local-anchor requirement
* Add private infrastructure assumptions
* Add shutdown/restart in v1
* Add target-side remote control without a dedicated scoped design
* Import or export secrets
* Auto-upload diagnostics
* Add analytics by default

## Technical Warnings to Preserve

### Ethernet vs Wi-Fi

Ethernet should be recommended for reliable Wake-on-LAN.

Wi-Fi wake may work on some laptops and devices, but it depends on hardware, firmware, drivers, power state, and OS behavior.

### Sleep vs Shutdown

Wake from sleep is generally more reliable than boot from full shutdown.

Boot from shutdown depends on BIOS/UEFI, NIC firmware, motherboard support, and operating-system power settings.

### Cellular Wake

Remote wake over cellular requires a valid home-side path.

Valid future paths may include:

* Home relay server
* Home device relay
* Router or VPN path
* Smart plug boot assist
* Supported smart-home integration

PowerBridge must not imply that cellular wake works by magic or through a public cloud alone.

## Public Release Guardrails

Before any public release:

* Remove generated local build artifacts from release packages.
* Exclude local output JSON and QR files.
* Exclude diagnostics ZIPs.
* Exclude local logs and caches.
* Verify source/docs do not contain private values.
* Verify examples use placeholders only.
* Verify no personal infrastructure appears in docs, UI, defaults, screenshots, or examples.
* Verify Windows Companion output is ignored by source control.
* Verify Android diagnostics are shared only by user action.
