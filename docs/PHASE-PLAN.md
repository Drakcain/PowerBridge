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

* `Old Android Device`
* `Alexa / Google Voice Devices`
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
* `Phase 12` - Home Device Relay architecture
* `Phase 13` - Home Device Relay prototype planning and contract validation
* `Phase 13.5` - Pre-prototype cleanup and Phase 14 readiness audit
* `Phase 14A` - Home Device Relay AIO correction + skeleton integration
* `Phase 14B` - Guided wake setup + method selection framework
* `Phase 14C` - Wake-path readiness + honest method wiring
* `Phase 14C.1` - Guided setup polish + Home Relay sharing + CI/version hygiene
* `Phase 14D` - Main APK method guides and readiness follow-through
* `Phase 14E` - Three-method scope reduction and old-Android preparation

## Next Phases

* `Phase 14F` - Old Android relay runtime prototype
* `Phase 15` - Alternate wake paths and guides
* `Phase 16` - Packaging and release prep follow-through

Phase status:

* `Phase 12` is complete
* `Phase 13` is complete as contract/planning work
* `Phase 13.5` is complete as cleanup/readiness work
* `Phase 14A` is complete as the AIO relay-mode correction inside the main Android app
* `Phase 14B` is complete as guided wake setup and plain-language method selection
* `Phase 14C` is complete as honest wake-path readiness wiring for `Home Wi-Fi only`, `Away from home`, and `Not sure`
* `Phase 14C.1` is complete as guided setup final-confirm verification, Home Relay text sharing, CI path filters, and phase-level version alignment
* `Phase 14D` is complete as main-APK method guide and readiness realignment
* `Phase 14E` is complete as the three-method scope reduction and old-Android preparation pass

Phase 14A scope completed:

* relay prototype direction corrected to the main Android AIO app
* launchable `Home Relay Mode` UI inside the main Android app
* local-only transport placeholders
* first-pass local diagnostics/logging
* contract-model placeholders
* main Android app icon updated

Phase 14A intentionally does not include:

* Firebase
* FCM
* cloud broker
* production auth
* real pairing
* real controller-to-relay transport
* TV runtime target
* smart plug or smart-home paths
* shutdown/restart/remote control

Phase 14B scope completed:

* main Android app now has a clear `Setup Help` entry point
* users can choose where wake should work without networking jargon
* app recommends `Home Wi-Fi`, `Old Android Device`, or `My Own Server`
* live methods can be selected from guided setup without risky broad auto-save
* `Home Relay Mode` remains clearly marked as not fully active yet

Phase 14C scope completed:

* `Home Wi-Fi only` is now a first-class guided setup path
* remote method families are shown only under `Away from home`
* `Not sure` now gives a plain-language recommendation instead of a technical list
* final setup status now shows `Ready now`, `Needs setup`, `Prototype`, `Coming later`, or `Advanced`
* only live paths can look ready in guided setup

Phase 14C.1 scope completed:

* guided setup final confirmation path verified and kept intact
* `Home Relay Mode` now supports `Share Report` with the Android share sheet
* GitHub Actions now use path filters so Android and Windows checks only run when their side changes
* development versioning now follows the phase-level 0.7.x progression

Phase 14D scope superseded by Phase 14E:

* `My Own Server` advanced-path clarity and readiness guidance
* broader method guidance reviewed and reduced

Phase 14E scope completed:

* removed unsupported media-device relay experiments from active product scope
* removed hardware power-cycle workaround experiments from active product scope
* removed router/VPN as a separate product method
* retained only `Old Android Device`, `Alexa / Google Voice Devices`, and `My Own Server` as remote method families

Phase 14F should focus on:

* old Android relay runtime
* Firebase / FCM
* cloud broker
* QR pairing for relay setup
* diagnostics on both controller and relay sides

See:

* [README.md](README.md)
* [HOME-DEVICE-RELAY-ARCHITECTURE.md](HOME-DEVICE-RELAY-ARCHITECTURE.md)
* [HOME-DEVICE-RELAY-CONTRACTS.md](HOME-DEVICE-RELAY-CONTRACTS.md)
* [HOME-DEVICE-RELAY-PROTOTYPE-PLAN.md](HOME-DEVICE-RELAY-PROTOTYPE-PLAN.md)

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
* that FCM, Firebase, or a cloud broker already exists in PowerBridge

Detailed component phase notes remain in:

* [android/docs/PHASE-PLAN.md](../android/docs/PHASE-PLAN.md)
* [windows-companion/docs/PHASE-PLAN.md](../windows-companion/docs/PHASE-PLAN.md)
