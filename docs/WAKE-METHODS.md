# PowerBridge Wake Methods

PowerBridge now uses a stripped method model.

The goal is not to support every possible device. The goal is a small set of paths that can become reliable and understandable for non-technical users.

## Approved Model

Local home-network wake remains the core built-in path:

| Path | User Label | Current Status | Purpose |
| --- | --- | --- | --- |
| Same home network | Local Wi-Fi Wake | Live | Main phone sends the local Wake-on-LAN packet while on the same LAN as the PC |

Remote wake is limited to three method families:

| Remote Method | User Label | Current Status | Purpose |
| --- | --- | --- | --- |
| Old Android device | Old Android Device | Prototype | Spare phone or tablet stays plugged in at home and sends the local wake packet |
| Voice ecosystem | Alexa / Google Voice Devices | Research | Official Alexa/Google-style voice ecosystem path where a safe wake integration is possible |
| Always-on server | My Own Server | Live for advanced users | User-owned NAS, Raspberry Pi, router, Docker host, server, or always-on PC performs the local wake action |

## Removed From Scope

These paths are intentionally removed from the active product model:

* unsupported media-device relay experiments
* hardware power-cycle workaround experiments
* router/VPN as a separate app method
* Broad “try every possible relay hardware” support

Reason:

* too much platform variance
* too much user confusion
* not reliable enough as a beginner foundation
* not required for the first clean public product path

## Product Rule

Do not add a new method family unless it can meet all of these:

* normal user can understand what to install
* setup can be guided inside PowerBridge
* failure modes can be explained clearly
* runtime can be tested on real hardware
* diagnostics can identify which step failed

## Next Runtime Direction

The next practical runtime target should be `Old Android Device`.

Expected shape:

```text
Main phone app
Old Android device at home
QR pairing
Visible relay/foreground state on the old device
Secure request from main phone
Local Wake-on-LAN packet sent by old device
Diagnostics on both sides
```

Do not start Alexa/Google runtime or broader server tooling until the old-Android relay workflow is clear.
