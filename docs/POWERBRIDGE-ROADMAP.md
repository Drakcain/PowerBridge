# PowerBridge Roadmap Reset

## Product Shape

PowerBridge is a two-part public product:

1. Android AIO app
2. Windows Companion setup helper

The Android app is the main user-facing product.
The Windows Companion remains in scope as the PC detection and QR setup helper.

## Current Version Truth

* Latest public release: `v0.6.1`
* Current development version: `0.7.5`
* Final public target: `v1.0.0`

## Current Runtime Truth

Live now:

* `Home Wi-Fi only` -> `Local Wi-Fi Wake`
* `My Own Server` -> `Home Relay Server`

Prototype or partial:

* `Home Relay Mode`
* `Old Phone / Tablet Relay` concept

Coming later or not live:

* `Fire TV / Smart TV Relay`
* `Smart Speaker Wake`
* `Smart Plug Power-On`
* Firebase / FCM
* cloud broker
* smart-home runtime
* TV runtime
* old phone / tablet relay runtime

## Consumer Setup Flow

1. Install PowerBridge.
2. Run Windows Companion.
3. Scan QR on Android.
4. Confirm the detected PC.
5. Choose where wake should work.
6. See what is ready now versus what still needs setup.
7. Use Wake or Boot from the main screen.

## Current Phase Table

* `Phase 14A` - Android AIO foundation and Home Relay Mode integration
* `Phase 14B` - Guided setup and welcome flow
* `Phase 14C` - Honest wake-method readiness wiring
* `Phase 14C.1` - Final confirm, Home Relay Share Report, GitHub Actions cleanup, version alignment
* `Phase 14D` - Main APK method guides and readiness follow-through
* `Phase 15` - Alternate wake paths and deeper runtime work where chosen
* `Phase 16` - Packaging, signing, release hardening, and public release preparation

## Deferred Work

Deferred for later phases:

* old phone / tablet relay runtime
* Firebase / FCM
* cloud broker
* Fire TV runtime
* smart plug vendor API integrations
* smart speaker vendor integrations
* second Android APK

## Versioning Rule

* `Phase 14D` -> `0.7.5`
* `Phase 14E` -> `0.7.6`
* `Phase 15` -> `0.8.0`
* `Phase 16` -> `0.9.0`
* final public release -> `1.0.0`

## Private Setup Boundary

PowerBridge must not include:

* legacy donor-app wording in public product behavior
* private hardware-brand wording tied to the developer setup
* private domains
* private IPs
* private MACs
* real relay tokens
* personal infrastructure assumptions

`Home Relay Server` always means a relay owned and controlled by the user.
