# PowerBridge Phase Plan

This root phase plan summarizes the public project as a combined Android app plus Windows setup companion.

## Current Public Truth

Live now:

* Android PC profiles
* `Local Wi-Fi Wake`
* `Home Relay Server`
* Windows Companion QR setup generation
* Android QR import preview and profile creation/update
* Diagnostics ZIP sharing by explicit user action

Not live yet:

* `Home Device Relay`
* `Smart Plug Boot Assist`
* `Smart Home Wake`
* `Advanced Network Setup` guides
* Shutdown/restart/hibernate
* Target-side remote control

## Completed Phases

* `Phase 1` - WakeMethod foundation
* `Phase 2` - Real Local Wi-Fi Wake
* `Phase 3` - Local Setup Assist
* `Phase 4` - Public setup polish and guardrails
* `Phase 5` - Local-first default and placeholder guardrails
* `Phase 6` - PC Profiles foundation
* `Phase 7` - Android setup import contract
* `Phase 8A` - Windows Companion PowerShell prototype
* `Phase 8B` - Windows Companion GUI and QR generation
* `Phase 9` - AIO pairing polish
* `Phase 10` - Setup and method UX cleanup
* `Phase 10.5` - Public repo and release prep
* `Phase 10.6` - Updater and release packaging
* `Phase 11` - Android release signing foundation

## Next Phases

* `Phase 12` - Home Device Relay architecture
* `Phase 13` - Home Device Relay prototype entry
* `Phase 14` - Alternate wake paths and guides
* `Phase 15` - Packaging and release prep follow-through

Phase 12 is architecture-only.

See:

* [HOME-DEVICE-RELAY-ARCHITECTURE.md](HOME-DEVICE-RELAY-ARCHITECTURE.md)

## Rules

PowerBridge must stay:

* public
* generic
* secret-free by default
* honest about network limits
* free of developer-specific infrastructure

PowerBridge must not claim:

* that cellular wake works without a home-side path
* that Windows Companion wakes directly
* that every PC can wake from every power state
* that unimplemented methods are already live
* that Fire TV, Android TV, or Google TV relay behavior is already proven reliable
* that FCM, Firebase, or a cloud broker already exists in PowerBridge

Detailed component phase notes remain in:

* [android/docs/PHASE-PLAN.md](../android/docs/PHASE-PLAN.md)
* [windows-companion/docs/PHASE-PLAN.md](../windows-companion/docs/PHASE-PLAN.md)
