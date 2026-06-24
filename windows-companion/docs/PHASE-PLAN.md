# PowerBridge Windows Companion Phase Plan

PowerBridge Windows Companion is a Windows-side setup helper for the PowerBridge Android app. It detects local PC network values, generates a `powerbridge.local_setup.v1` setup payload, and displays a scan-safe QR code that Android can import.

Windows Companion is not a wake method, remote relay, Windows service, or background agent.

## Current Product Truth

Windows Companion currently supports:

* Adapter-first pairing flow
* Windows adapter detection
* Local setup value detection
* `powerbridge.local_setup.v1` JSON generation
* Scan-safe black-on-white QR generation
* CLI workflow
* Themed WPF GUI
* Secret-free setup payloads
* No-admin operation
* No-cloud operation
* No-background operation

Windows Companion does not currently support:

* Direct PC wake from Windows
* Windows wake receiver mode
* Windows service installation
* Windows background agent behavior
* Remote relay behavior
* Cloud relay behavior
* Smart plug control
* Smart-home control
* Router automation
* Firewall automation
* BIOS, UEFI, NIC, or Windows power setting changes

## Supported Setup Role

Windows Companion prepares setup data for Android `Local Wi-Fi Wake`.

It detects:

* PC name
* Adapter type
* MAC address
* IPv4 address
* Gateway
* Subnet prefix
* Broadcast IP

It generates:

* A `powerbridge.local_setup.v1` JSON payload
* A scan-safe black-on-white QR code
* Setup data Android can preview before saving

It helps Android:

* Create a new PC profile
* Update the current PC profile
* Configure `Local Wi-Fi Wake`
* Avoid manual typing of MAC, IP, gateway, and broadcast values

## Honest Limits

Windows Companion prepares setup data only.

It does not:

* Wake the PC directly
* Bypass NAT or firewall boundaries
* Remove the need for a home anchor when using off-site or cellular wake
* Install a service
* Run in the background
* Require admin rights
* Change Windows power settings
* Change BIOS or UEFI settings
* Change NIC settings
* Change firewall rules
* Change router settings
* Collect credentials, passwords, relay tokens, API keys, or secrets

Remote or cellular wake still requires a valid home-side path, such as:

* A user-owned relay
* An always-on home device
* A router or VPN path
* A smart plug or hardware bypass
* A supported smart-home integration

## Completed Phases

### Phase 8 — Windows Companion Prototype and GUI

Built the first Windows-side setup helper.

Completed work:

* CLI prototype
* Adapter detection
* Local network value detection
* JSON payload generation
* QR generation
* GUI pairing helper
* Basic validation script

### Phase 9 — Pairing Polish

Polished the Windows-to-Android setup flow.

Completed work:

* Adapter-first GUI flow
* Detect-before-generate guardrail
* Scan-safe QR tuning
* Android import wording alignment
* Reviewed Android import flow alignment
* Create/update profile flow alignment
* Secret-free payload preservation
* GUI wording cleanup

### Phase 10 — Setup + Method UX Alignment

Aligned Windows Companion wording with the broader PowerBridge method taxonomy.

Completed work:

* Described Windows Companion as a setup helper
* Described Windows Companion as a `Local Wi-Fi Wake` profile generator
* Removed wording that could imply Windows Companion is a wake method
* Removed wording that could imply Windows Companion is a relay
* Preserved the NAT/WOL boundary
* Preserved scan-safe black-on-white QR behavior

## Relationship to PowerBridge Android

Windows Companion should be described as:

* Setup helper
* Pairing helper
* Local setup detector
* `Local Wi-Fi Wake` profile generator
* QR import source for Android

Windows Companion should not be described as:

* A wake method
* A remote relay
* A Windows agent
* A remote-wake solution by itself
* A smart-home integration
* A smart plug integration
* A target-side control tool

## Current Pairing Flow

1. Open PowerBridge Windows Companion.
2. Select the adapter to pair.
3. Click `Detect This PC`.
4. Confirm detected values.
5. Click `Generate QR Code`.
6. Open PowerBridge on Android.
7. Go to `Settings -> PC Profiles -> Scan Setup QR`.
8. Scan the QR.
9. Review the Android import preview.
10. Choose `Create New Profile` or `Update Current Profile`.
11. Confirm the imported profile uses `Local Wi-Fi Wake`.
12. Run `Check Local Setup` on Android.

## QR Contract

Windows Companion generates the same setup payload accepted by Android import:

```text
powerbridge.local_setup.v1
```

The contract is setup-only.

It must not include:

* Passwords
* Relay tokens
* API keys
* Private keys
* Windows credentials
* Remote desktop credentials
* WinRM credentials
* SSH credentials
* Cloud credentials

The QR should remain:

* Plain JSON
* Scan-safe
* Black-on-white
* Secret-free
* Compatible with Android import

## Output Hygiene

Generated output may contain real local network values.

Machine-local output must not be committed or published, including:

* Generated JSON payloads
* Generated QR images
* Real MAC addresses
* Real local IP addresses
* Real gateways
* Real broadcast IPs
* Diagnostics ZIPs
* Local logs
* Local caches

Public examples should use placeholders only.

## Condensed PowerBridge Roadmap

| Phase      | Name                             | Purpose                                                        |
| ---------- | -------------------------------- | -------------------------------------------------------------- |
| `Phase 10` | Setup + Method UX Cleanup        | Clean method names, setup wording, warnings, and public docs   |
| `Phase 11` | Home Device Relay Architecture   | Design the old-phone/tablet/TV relay path                      |
| `Phase 12` | Home Device Relay Prototype      | Build the first home-device relay implementation               |
| `Phase 13` | Alternate Wake Paths + Guides    | Smart plug, smart-home, NAS, router, and advanced setup guides |
| `Phase 14` | Project Packaging + Release Prep | Repo structure, shared contracts, release packaging, checksums |

## Future Relationship to Home Device Relay

Windows Companion is not the Home Device Relay.

Future Home Device Relay work should focus on Android-based or home-anchor devices, such as:

* Old Android phones
* Android tablets
* Fire TV
* Android TV
* Google TV

Windows Companion may remain useful for preparing PC setup data, but it should not become the main always-on relay path unless a separate design phase intentionally changes that scope.

## Guardrails

Windows Companion must remain:

* Public-safe
* Generic
* Setup-only
* Secret-free
* No-admin by default
* No-service by default
* No-background by default
* Honest about Wake-on-LAN limitations

Future work must not silently add:

* A Windows service
* A background agent
* A wake receiver
* Cloud relay behavior
* Smart plug control
* Smart-home control
* Router automation
* Firewall automation
* Credential import/export
* Secret storage
* Target-side remote control
