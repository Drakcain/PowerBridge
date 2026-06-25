# Home Device Relay Shared Contracts

This folder is reserved for future shared contract assets related to PowerBridge home-side relay work.

Current status:

* docs-first only
* no runtime Kotlin models yet
* no generated schemas yet
* no transport code yet
* no cloud broker contracts yet
* no Firebase / FCM contracts yet
* no relay-agent implementation yet

## Purpose

This folder exists to keep future relay contract material organized before runtime code is extracted or shared across modules.

Intended future use:

* shared schema notes
* shared Kotlin data models
* shared validation helpers
* contract test fixtures
* placeholder-only sample payloads
* relay request / relay response contract notes
* compatibility notes between Android client and future relay components

## Current PowerBridge Method Model

PowerBridge’s approved remote wake model is limited to:

* **Old Android Device** — an old Android phone or tablet left at home as a relay device
* **My Own Server / Home Relay Server** — a user-owned always-on PC, server, NAS, Raspberry Pi, Docker host, or similar home-side device
* **Alexa / Google Voice Devices** — a future smart-home integration path

Local same-network wake remains:

* **Local Wi-Fi Wake**

## Not In Scope

This folder must not reintroduce removed method families.

Do not add shared contracts for:

* TV / streaming relay
* Fire TV / Android TV relay
* Roku relay
* smart plug / hardware power-cycle boot
* router / VPN as a standalone method
* shutdown / restart workflows
* remote desktop control
* target-side command execution

If these topics appear in future docs, they should be described only as removed, rejected, or out-of-scope.

## Public-Safety Rules

Do not place real user or machine-specific values in this folder.

Do not include:

* real relay tokens
* passwords
* API keys
* credentials
* private keys
* signing material
* real MAC addresses
* real private IP addresses
* real gateway values
* real broadcast addresses
* real relay endpoints
* personal domains
* developer-specific infrastructure values
* generated diagnostics ZIPs
* live QR payloads
* real setup JSON payloads
* real local network captures
* screenshots containing live setup values

Use placeholder-only data for examples and fixtures.

## Safe Placeholder Values

Use values like these in examples:

```json
{
  "pcName": "Gaming PC",
  "targetMac": "AA:BB:CC:DD:EE:FF",
  "targetIp": "192.168.1.100",
  "gateway": "192.168.1.1",
  "broadcastIp": "192.168.1.255",
  "wakePort": 9,
  "relayId": "example-relay",
  "requestId": "example-request-id"
}
```

Do not use real values copied from local diagnostics, Windows Companion output, Android profiles, screenshots, logs, or QR payloads.

## Extraction Rule

Runtime implementation should stay out of this folder until a deliberate shared-contract extraction is approved.

Before adding runtime-facing files here, confirm:

* the contract is needed by more than one component
* the data model is stable enough to share
* the fields are secret-free by design
* examples use placeholders only
* compatibility expectations are documented
* Android client behavior is not broken
* Windows Companion setup behavior is not changed unintentionally

## Future Contract Requirements

Any future relay contract should clearly define:

* schema name
* schema version
* required fields
* optional fields
* forbidden fields
* request direction
* response direction
* error behavior
* compatibility expectations
* placeholder examples
* public-safety rules

Future contracts must not include:

* credentials
* shared relay tokens
* developer-managed infrastructure defaults
* hidden command execution fields
* shutdown or restart commands
* remote desktop credentials
* router credentials
* cloud secrets
* signing material

## Compatibility Rule

Future contracts should be versioned explicitly.

Use names such as:

```text
powerbridge.home_relay.request.v1
powerbridge.home_relay.response.v1
powerbridge.relay_agent.status.v1
```

Do not silently change the meaning of an existing contract once Android or relay components depend on it.

## Added Recommendations

Before adding files to this folder:

* confirm the file is a contract, fixture, or contract note
* confirm it does not contain live local values
* confirm it does not contain secrets
* confirm it does not revive removed methods
* confirm it does not imply a runtime exists before it does
* confirm placeholders are obvious and safe
* confirm the contract fits the approved PowerBridge method model
