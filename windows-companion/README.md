# PowerBridge Windows Companion

PowerBridge Windows Companion is a Windows-side setup helper for the PowerBridge Android app.

It detects local PC network details, builds a `powerbridge.local_setup.v1` setup payload, and displays a scan-safe QR code that Android can import.

Windows Companion is **not** a wake method by itself. It prepares setup data for PowerBridge wake profiles.

## What It Does

Windows Companion helps Android create or update a local PC wake profile by detecting values from the selected Windows network adapter.

It detects:

* PC name
* adapter type
* MAC address
* IPv4 address
* gateway
* subnet prefix
* broadcast IP

It generates:

* `powerbridge.local_setup.v1` JSON
* scan-safe black-on-white QR code
* setup data for a `Local Wi-Fi Wake` profile on Android

The generated setup profile may also support future relay setup flows where another user-owned home device needs the same PC wake information.

## What It Does Not Do

Windows Companion does not:

* wake the PC directly
* run as a wake relay
* run in the background
* install a Windows service
* require administrator rights
* change firewall settings
* change router settings
* change BIOS, UEFI, NIC, or Windows power settings
* solve remote wake over cellular by itself
* collect credentials, passwords, relay tokens, API keys, or secrets
* provide shared relay infrastructure
* provide developer-managed infrastructure

## GUI Setup Flow

1. Open PowerBridge Windows Companion.
2. Select the adapter you want to pair.
3. Click `Detect This PC`.
4. Confirm the detected values.
5. Click `Generate QR Code`.
6. On Android, open `PowerBridge -> Settings -> PC Profiles -> Scan Setup QR`.
7. Scan the QR code.
8. Review the import preview on Android.
9. Choose `Create New Profile` or `Update Current Profile`.
10. Confirm the imported profile uses `Local Wi-Fi Wake`.

## Android Result

After scanning the QR, PowerBridge Android should show an import preview before saving.

The preview should include:

* source
* created time
* PC name
* adapter type
* masked MAC address
* IPv4 address
* gateway
* broadcast IP

The Android app should not accept relay tokens, passwords, credentials, API keys, or secrets through the local setup QR contract.

## Technical Boundary

Windows Companion prepares setup data only.

It does not bypass the Wake-on-LAN network boundary. Off-site or cellular wake still requires a valid user-owned home-side path that can perform the final local wake packet send inside the home network.

Approved PowerBridge remote paths are:

* **Old Android Device** — an old Android phone or tablet left at home as a relay device
* **My Own Server / Home Relay Server** — a user-owned always-on PC, server, NAS, Raspberry Pi, Docker host, or similar home-side device
* **Alexa / Google Voice Devices** — a future smart-home integration path

A public cloud service cannot directly inject a Wake-on-LAN Magic Packet into a private home LAN unless a local home-side anchor or relay performs the final local packet send.

## Removed From Active Scope

Windows Companion should not describe the following as active PowerBridge wake methods:

* TV / streaming relay
* Fire TV / Android TV relay
* Roku relay
* smart plug / hardware power-cycle boot
* router / VPN as a standalone PowerBridge method
* shutdown / restart workflows

PowerBridge may document advanced networking concepts separately, but Windows Companion should remain a setup helper, not a broad network-management tool.

## Ethernet and Wi-Fi Notes

Ethernet is recommended for the most reliable Wake-on-LAN behavior.

Wi-Fi wake may work on some laptops and devices, but it depends on hardware, firmware, driver support, power state, and operating-system behavior.

Boot from full shutdown is often less reliable than wake from sleep because BIOS/UEFI, NIC firmware, motherboard support, Windows Fast Startup, and power delivery to the network adapter can affect the result.

## Commands

Run these from the Windows Companion project root.

```powershell
Set-Location "<repo-root>\windows-companion"
```

Run validation tests:

```powershell
pwsh -NoProfile -ExecutionPolicy Bypass -File ".\scripts\Test-PowerBridgeCompanion.ps1"
```

List available adapters:

```powershell
pwsh -NoProfile -ExecutionPolicy Bypass -File ".\src\PowerBridgeCompanion.ps1" -ListAdapters
```

Generate setup JSON from the command line:

```powershell
pwsh -NoProfile -ExecutionPolicy Bypass -File ".\src\PowerBridgeCompanion.ps1" -NoClipboard
```

Open the GUI:

```powershell
pwsh -NoProfile -ExecutionPolicy Bypass -File ".\scripts\Start-PowerBridgeCompanionGui.ps1"
```

Build the Windows Companion installer:

```powershell
pwsh -NoProfile -ExecutionPolicy Bypass -File ".\scripts\Build-PowerBridgeCompanionInstaller.ps1"
```

## Output Hygiene

Generated output may contain real local machine network values.

Treat generated JSON and QR files as machine-local setup data. Do not publish them in screenshots, issue reports, release packages, documentation examples, or public repositories.

Recommended rules:

* keep generated output ignored by source control
* do not commit generated JSON payloads
* do not commit generated QR images
* do not publish real MAC addresses, local IP addresses, gateways, or broadcast addresses
* do not publish screenshots showing real setup values
* use placeholders in public examples

GUI mode keeps the QR available for the current session. CLI mode may write JSON or PNG output for testing and troubleshooting.

Generated output belongs in `windows-companion/output/`, which should stay ignored except for `.gitkeep`.

## Installed Release Behavior

Installed releases may add a Start Menu shortcut for `Check for PowerBridge Updates`.

The GUI may also expose `Check for Updates`.

Update checks must not collect secrets, local network values, generated setup payloads, diagnostics ZIPs, or QR images.

## Public Release Notes

Windows Companion is distributed as a setup helper, not as a wake engine.

Public repo rules:

* keep generated output ignored
* do not publish real QR screenshots with live values
* do not publish real JSON payloads with live values
* do not claim the companion wakes the PC directly
* do not claim remote or cellular wake works without a home-side path
* do not claim unsigned Windows packages are fully trusted or production-signed
* clearly warn users if Windows SmartScreen or “unknown publisher” prompts may appear

## Relationship to PowerBridge Methods

Windows Companion is a setup helper for `Local Wi-Fi Wake`.

The same detected PC values may also be useful for future user-owned relay setup, such as the `Old Android Device` path or `My Own Server / Home Relay Server` path.

Windows Companion is not:

* `Old Android Device`
* `Home Relay Server`
* `Smart Home Wake`
* a remote wake broker
* a cloud service
* a Windows service
* a wake engine
* a shutdown/restart tool
* a router/VPN configuration tool

The Android app imports the generated setup and performs wake actions using the selected PowerBridge method.

## Public-Safety Rules

Windows Companion must remain:

* generic
* local-first
* secret-free
* public-safe
* setup-only

It must not ship with:

* personal machine values
* private relay assumptions
* private domains
* private IP addresses
* private MAC addresses
* tokens or token-like strings
* user-specific absolute paths in public docs
* generated local output in source control

## Added Recommendations

Before publishing a Windows Companion release:

* run `scripts\Test-PowerBridgeCompanion.ps1`
* test adapter selection on at least one Ethernet and one Wi-Fi adapter if available
* verify QR generation still uses scan-safe black-on-white output
* verify generated JSON and QR files stay ignored
* verify the GUI does not persist QR output longer than intended
* verify no real local values appear in screenshots or docs
* verify installer output is not committed unless intentionally published as a release asset
* clearly label unsigned early packages if code signing is not available
* keep Windows Companion described as setup-only across README, install notice, release notes, and docs

## Related Docs

* [../README.md](../README.md)
* [../BUILD.md](../BUILD.md)
* [../INSTALL-NOTICE.txt](../INSTALL-NOTICE.txt)
* [../docs/QR-CONTRACT.md](../docs/QR-CONTRACT.md)
* [../docs/WAKE-METHODS.md](../docs/WAKE-METHODS.md)
* [docs/PHASE-PLAN.md](docs/PHASE-PLAN.md)
