# PowerBridge Windows Companion Phase Plan

PowerBridge Windows Companion is a Windows-side setup helper for the PowerBridge Android app.

It detects local PC network values, generates a `powerbridge.local_setup.v1` setup payload, and displays a scan-safe QR code that Android can import.

Windows Companion is **not** a wake method, remote relay, Windows service, background agent, cloud relay, or wake receiver.

## Current Product Truth

Windows Companion currently supports:

* adapter-first pairing flow
* Windows adapter detection
* local setup value detection
* `powerbridge.local_setup.v1` JSON generation
* scan-safe black-on-white QR generation
* CLI workflow
* themed WPF GUI
* secret-free setup payloads
* no-admin operation
* no-cloud operation
* no-background operation

Windows Companion does not currently support:

* direct PC wake from Windows
* Windows wake receiver mode
* Windows service installation
* Windows background agent behavior
* remote relay behavior
* cloud relay behavior
* smart plug control
* smart-home control
* router automation
* firewall automation
* BIOS, UEFI, NIC, or Windows power setting changes
* credential collection
* token storage
* target-side command execution

## Supported Setup Role

Windows Companion prepares setup data for Android `Local Wi-Fi Wake`.

It detects:

* PC name
* adapter type
* MAC address
* IPv4 address
* gateway
* subnet prefix
* broadcast IP

It generates:

* `powerbridge.local_setup.v1` JSON payload
* scan-safe black-on-white QR code
* setup data Android can preview before saving

It helps Android:

* create a new PC profile
* update the current PC profile
* configure `Local Wi-Fi Wake`
* avoid manual typing of MAC, IP, gateway, and broadcast values

The same detected PC values may also be useful later for user-owned home relay setup, but Windows Companion itself remains setup-only.

## Honest Limits

Windows Companion prepares setup data only.

It does not:

* wake the PC directly
* bypass NAT or firewall boundaries
* remove the need for a home-side anchor when using off-site or cellular wake
* install a service
* run in the background
* require admin rights
* change Windows power settings
* change BIOS or UEFI settings
* change NIC settings
* change firewall rules
* change router settings
* collect credentials, passwords, relay tokens, API keys, or secrets

Remote or cellular wake still requires a valid user-owned home-side path, such as:

* **Old Android Device** — an old Android phone or tablet left at home as a relay device
* **My Own Server / Home Relay Server** — a user-owned always-on PC, server, NAS, Raspberry Pi, Docker host, or similar home-side device
* **Alexa / Google Voice Devices** — a future smart-home integration path

PowerBridge does not include a shared public relay service, shared relay token, or developer-managed wake infrastructure.

## Completed Windows Companion Phases

### Phase 8 — Windows Companion Prototype and GUI

Built the first Windows-side setup helper.

Completed work:

* CLI prototype
* adapter detection
* local network value detection
* JSON payload generation
* QR generation
* GUI pairing helper
* basic validation script

### Phase 9 — Pairing Polish

Polished the Windows-to-Android setup flow.

Completed work:

* adapter-first GUI flow
* detect-before-generate guardrail
* scan-safe QR tuning
* Android import wording alignment
* reviewed Android import flow alignment
* create/update profile flow alignment
* secret-free payload preservation
* GUI wording cleanup

### Phase 10 — Setup + Method UX Alignment

Aligned Windows Companion wording with the broader PowerBridge method model.

Completed work:

* described Windows Companion as a setup helper
* described Windows Companion as a `Local Wi-Fi Wake` profile generator
* removed wording that could imply Windows Companion is a wake method
* removed wording that could imply Windows Companion is a relay
* preserved the NAT/Wake-on-LAN boundary
* preserved scan-safe black-on-white QR behavior

### Phase 14E — Scope Reset Alignment

Updated the Windows Companion boundary after PowerBridge stripped unreliable remote method families.

Completed alignment:

* kept Windows Companion setup-only
* kept Windows Companion secret-free
* kept Windows Companion local-first
* removed unreliable remote method drift from active scope
* clarified that remote wake requires a user-owned home-side path
* aligned wording with the approved remote method model

## Relationship to PowerBridge Android

Windows Companion should be described as:

* setup helper
* pairing helper
* local setup detector
* `Local Wi-Fi Wake` profile generator
* QR import source for Android

Windows Companion should not be described as:

* a wake method
* a remote relay
* a Windows agent
* a remote-wake solution by itself
* a smart-home integration
* a smart plug integration
* a router/VPN setup method
* a target-side control tool

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

Windows Companion generates the setup payload accepted by Android import:

```text
powerbridge.local_setup.v1
```

The contract is setup-only.

It must not include:

* passwords
* relay tokens
* API keys
* private keys
* Windows credentials
* remote desktop credentials
* WinRM credentials
* SSH credentials
* cloud credentials
* shared relay credentials
* developer-managed infrastructure values

The QR should remain:

* plain JSON
* scan-safe
* black-on-white
* secret-free
* compatible with Android import
* safe to preview before saving

## Output Hygiene

Generated output may contain real local network values.

Machine-local output must not be committed or published, including:

* generated JSON payloads
* generated QR images
* real MAC addresses
* real local IP addresses
* real gateways
* real broadcast IPs
* diagnostics ZIPs
* local logs
* local caches
* screenshots showing live setup values

Public examples should use placeholders only.

## Current Condensed PowerBridge Roadmap

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

* `Phase 14E` is complete as the approved-method reset.
* `Phase 14F` is the next runtime foundation phase.
* The next runtime target is `Old Android Device`.
* Windows Companion remains a setup helper and should not become the runtime relay.

## Future Relationship to Old Android Relay

Windows Companion is not the Old Android Relay.

Future Old Android Relay work should focus on a home-side Android phone or tablet that can:

* import or receive PC setup values
* stay available at home
* send the final local Wake-on-LAN packet
* report readiness and diagnostics
* eventually receive a remote wake request through a selected user-owned path

Windows Companion may remain useful for preparing PC setup data, but it should not become the main always-on relay path unless a separate design phase intentionally changes that scope.

## Removed From Active Scope

Windows Companion and PowerBridge docs should not describe these as active remote wake methods:

* TV / streaming relay
* Fire TV / Android TV relay
* Roku relay
* smart plug / hardware power-cycle boot
* router / VPN as a standalone method
* shutdown / restart workflows

These may appear only as rejected, removed, or out-of-scope concepts where context is necessary.

## Guardrails

Windows Companion must remain:

* public-safe
* generic
* setup-only
* secret-free
* no-admin by default
* no-service by default
* no-background by default
* honest about Wake-on-LAN limitations

Future work must not silently add:

* a Windows service
* a background agent
* a wake receiver
* cloud relay behavior
* smart plug control
* smart-home control
* router automation
* firewall automation
* credential import/export
* secret storage
* target-side remote control
* shutdown/restart control

## Added Recommendations

Before future Windows Companion changes:

* confirm the change supports setup only
* confirm no secret fields are added to the QR contract
* confirm generated output stays ignored by Git
* confirm screenshots and docs use placeholder values only
* confirm wording does not imply Windows Companion wakes the PC directly
* confirm remote wake is described as requiring a user-owned home-side path
* confirm removed methods are not revived as active scope
* confirm Windows Companion remains aligned with the Android import flow
* confirm `powerbridge.local_setup.v1` stays backward-compatible unless a new contract version is intentionally created
