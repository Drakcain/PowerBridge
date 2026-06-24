# PowerBridge Windows Companion

PowerBridge Windows Companion is a Windows-side setup helper for the PowerBridge Android app. It detects local PC network details, builds a `powerbridge.local_setup.v1` setup payload, and displays a scan-safe QR code that Android can import.

Windows Companion is not a wake method by itself. It prepares setup data for `Local Wi-Fi Wake`.

## What It Does

Windows Companion helps Android create or update a local wake profile by detecting the selected Windows adapter.

It detects:

* PC name
* Adapter type
* MAC address
* IPv4 address
* Gateway
* Subnet prefix
* Broadcast IP

It generates:

* `powerbridge.local_setup.v1` JSON
* A scan-safe black-on-white QR code
* Setup data for a `Local Wi-Fi Wake` profile on Android

## What It Does Not Do

Windows Companion does not:

* Wake the PC directly
* Run in the background
* Install a Windows service
* Require administrator rights
* Change firewall settings
* Change router settings
* Change BIOS, UEFI, NIC, or Windows power settings
* Solve remote wake over cellular by itself
* Collect credentials, passwords, relay tokens, or secrets

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

After scanning the QR, PowerBridge Android should show a reviewed import preview before saving.

The preview should include:

* Source
* Created time
* PC name
* Adapter type
* Masked MAC address
* IPv4 address
* Gateway
* Broadcast IP

The Android app never accepts relay tokens, passwords, credentials, API keys, or secrets through the local setup QR contract.

## Technical Boundary

Windows Companion prepares setup data only.

It does not bypass the Wake-on-LAN network boundary. Off-site or cellular wake still requires a valid home-side path, such as:

* A user-owned relay
* An always-on home device
* A router or VPN path
* A smart plug or hardware bypass
* A supported smart-home integration

A public cloud service cannot directly inject a Wake-on-LAN Magic Packet into a private home LAN unless a local home-side anchor or relay performs the final local packet send.

## Ethernet and Wi-Fi Notes

Ethernet is recommended for the most reliable Wake-on-LAN behavior.

Wi-Fi wake may work on some laptops and devices, but it depends on hardware, firmware, driver support, power state, and operating-system behavior.

Boot from full shutdown is often less reliable than wake from sleep because BIOS/UEFI, NIC firmware, motherboard support, and Windows Fast Startup settings can affect the result.

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

Treat generated JSON and QR files as machine-local setup data. Do not publish them in screenshots, issue reports, release packages, or public repositories.

Recommended rules:

* Keep generated output ignored by source control.
* Do not commit generated JSON payloads.
* Do not commit generated QR images.
* Do not publish real MAC addresses, local IP addresses, gateways, or broadcast addresses.
* Use placeholders in public examples.

GUI mode keeps the QR available for the current session. CLI mode may write JSON or PNG output for testing and troubleshooting.

Installed releases add a Start Menu shortcut for `Check for PowerBridge Updates`, and the GUI also exposes `Check for Updates`.

## Public Release Notes

Windows Companion is distributed as a setup helper, not as a wake engine.

Public repo rules:

* keep generated output ignored
* do not publish real QR screenshots with live values
* do not publish real JSON payloads with live values
* do not claim the companion wakes the PC directly
* do not claim remote or cellular wake works without a home-side path

## Relationship to PowerBridge Methods

Windows Companion is a setup helper for `Local Wi-Fi Wake`.

It is not:

* `Home Relay Server`
* `Home Device Relay`
* `Smart Plug Boot Assist`
* `Smart Home Wake`
* `Advanced Network Setup`

It does not implement a wake engine. The Android app imports the generated setup and performs the wake action using the selected PowerBridge method.

## Public-Safety Rules

Windows Companion must remain:

* Generic
* Local-first
* Secret-free
* Public-safe
* Setup-only

It must not ship with:

* Personal machine values
* Private relay assumptions
* Private domains
* Private IP addresses
* Private MAC addresses
* Tokens or token-like strings
* User-specific absolute paths in public docs
* Generated local output in source control

## Related Docs

* [../README.md](../README.md)
* [../BUILD.md](../BUILD.md)
* [../docs/QR-CONTRACT.md](../docs/QR-CONTRACT.md)
* [docs/PHASE-PLAN.md](docs/PHASE-PLAN.md)
