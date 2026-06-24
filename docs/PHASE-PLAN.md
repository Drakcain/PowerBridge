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

## Next Phases

* `Phase 11` - Home Device Relay architecture
* `Phase 12` - Home Device Relay prototype
* `Phase 13` - Alternate wake paths and guides
* `Phase 14` - Packaging and release prep follow-through

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

Detailed component phase notes remain in:

* [android/docs/PHASE-PLAN.md](../android/docs/PHASE-PLAN.md)
* [windows-companion/docs/PHASE-PLAN.md](../windows-companion/docs/PHASE-PLAN.md)
