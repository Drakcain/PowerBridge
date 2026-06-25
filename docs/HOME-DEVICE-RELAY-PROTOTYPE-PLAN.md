# Home Device Relay Prototype Plan

## Phase 13 Status

Phase 13 is planning and contract validation only.

No relay runtime implementation is added in this phase.

## Phase 14A Status

Phase 14A is the AIO correction plus skeleton-only runtime foundation.

Completed in Phase 14A:

* moved the relay prototype direction into the main Android app
* minimal launchable `Home Relay Mode` UI
* local-only transport placeholder
* first-pass relay diagnostics view/logging
* contract model placeholders in code

Not implemented in Phase 14A:

* real pairing
* real controller-to-relay transport
* Firebase / FCM
* cloud broker
* production auth
* TV-specific runtime work
* actual relay-triggered Wake-on-LAN execution

Product-direction truth after Phase 14A:

* PowerBridge should be one Android AIO app
* Windows Companion remains the separate Windows app
* spare Android phones/tablets should use the same PowerBridge APK in `Home Relay Mode`

## Phase 14B Status

Phase 14B added the guided consumer setup layer before deeper relay runtime work.

Completed in Phase 14B:

* main Android app now has a plain-language `Setup Help` path
* users can choose where wake should work without seeing low-level network terms first
* app now recommends `Home Wi-Fi`, `Old Phone / Tablet Relay`, or `My Own Server`
* `Home Relay Mode` remains honest about prototype status

Not implemented in Phase 14B:

* real pairing
* real controller-to-relay transport
* real relay-triggered WOL execution
* Firebase / FCM
* cloud broker
* TV-specific runtime work

## Phase 14C Status

Phase 14C completed the setup-truth and readiness-wiring pass before deeper relay runtime.

Completed in Phase 14C:

* `Home Wi-Fi only` is now a first-class setup choice
* `Away from home` now gates the five remote method families behind a simpler first question
* `Not sure` now gives a plain recommendation instead of making users decode architecture terms
* guided setup now shows honest final states such as `Ready now`, `Needs setup`, `Prototype`, `Coming later`, and `Advanced`
* only currently live or advanced self-hosted paths can appear ready in guided setup

Not implemented in Phase 14C:

* real pairing
* real controller-to-relay transport
* real relay-triggered WOL execution
* Firebase / FCM
* cloud broker
* TV-specific runtime work

## Phase 14D Prototype Goal

Phase 14 should deliver the first narrow `Home Device Relay` prototype without trying to solve the full remote-wake product in one pass.

The prototype goal is:

* prove one relay device can be paired in a controlled test flow
* prove one controller can create a valid wake request
* prove one relay can validate that request
* prove one relay can send a local WOL packet for one linked PC profile
* prove diagnostics are clear enough to explain success and failure

## Recommended Target Device Order

Prototype order:

1. standard Android phone or tablet first
2. Fire tablet second
3. Fire TV / Android TV / Google TV later

Reason:

* normal Android devices are easier to test and debug first
* Fire tablets are useful but may add sideloading and platform-policy complexity
* TV-class devices should come after the basic relay contract and flow are proven

## Minimum Viable Prototype Flow

Recommended first prototype flow:

1. Install a relay prototype on a spare Android phone or tablet that stays on the same LAN as the target PC.
2. Pair the relay with the controller using a local-only QR/code flow or manually loaded placeholder pairing data.
3. Controller creates a local test wake request for a linked profile.
4. Relay validates the request and confirms profile authorization.
5. Relay sends the local Wake-on-LAN packet sequence.
6. Relay records diagnostics and a wake result.
7. Controller or tester reviews the result and failure reason if any.

Important prototype boundary:

* prefer local-only prototype transport first if that avoids cloud, push, or broker complexity

## Test Device Requirements

Recommended minimum test environment:

* one spare Android phone or tablet used as the relay
* one controller phone running the main PowerBridge app
* one target PC already verified to wake through `Local Wi-Fi Wake`
* stable same-LAN network path between relay and target PC
* relay device left on power during test sessions

Useful secondary targets later:

* one Fire tablet
* one Fire TV class device
* one Android TV or Google TV device if available

## Architecture Choice Recommendation

Recommended Phase 14D direction:

* keep the PowerBridge Android app as the single user-facing Android app
* keep relay behavior inside the same app under a clearly marked `Home Relay Mode`
* keep the Windows Companion separate

Why:

* preserves the intended one-APK Android product shape
* still keeps runtime scope narrow
* keeps relay diagnostics and placeholder transport visible without creating a second end-user app

## Package / App Strategy

Recommended package strategy:

* controller and relay prototype both live in the existing PowerBridge Android app
* contracts remain documented first, then can become shared Kotlin models inside the app as runtime work grows

## Feature Flags / Staged UI Guidance

Phase 14 should keep UI scope narrow.

Recommended guidance:

* hide unfinished relay features behind internal prototype toggles if needed
* do not expose a public-facing “fully works over cellular” promise
* keep pairing/testing surfaces obviously experimental until validated

## Diagnostics Requirements

Prototype diagnostics should answer:

* did the controller create a request
* did the relay receive it
* was the request authorized
* was the request expired
* did the relay attempt to send WOL
* what broadcast IP and UDP port were used
* what device/network limitation likely blocked success

At minimum, prototype diagnostics should record:

* request id
* relay device id
* profile id
* timestamps
* result code
* error category
* network state summary
* battery/background state summary

## Security Requirements

Phase 14 prototype must still respect the design boundaries:

* no raw unauthenticated public wake endpoint
* no PC passwords
* no OS credentials
* no long-lived secret examples in docs
* relay only wakes linked profiles
* request expiration and nonce support remain part of the design

Prototype shortcuts are acceptable only if they stay local-only, explicit, and non-public-facing.

## Platform Risks

Key Phase 14 risks:

* Doze and battery optimization may suppress background behavior
* OEM background policies may differ significantly
* Fire OS behavior may differ from standard Android
* TV navigation and TV foreground UX may be awkward
* always-on power does not guarantee the device remains ready to receive work
* UDP broadcast behavior must be validated on real hardware

## What Is Explicitly Not Built Yet

Phase 14 should not include:

* production cloud broker
* FCM production setup
* Firebase integration
* public accounts
* smart plug support
* smart-home support
* Fire TV as the first target
* Android TV as the first target
* Windows agent
* shutdown or restart
* remote desktop or remote control
* silent update
* analytics

## Success Criteria For Prototype Implementation

Phase 14 prototype should be considered successful only if it proves:

* one relay device can be identified and paired safely
* one linked PC profile can be authorized for wake
* one local test transport can deliver a valid request
* one relay can send WOL to one validated target profile
* result and diagnostics are understandable enough to troubleshoot failures

## Phase 14D Entry Criteria

Before implementation starts:

* [HOME-DEVICE-RELAY-ARCHITECTURE.md](HOME-DEVICE-RELAY-ARCHITECTURE.md) is accepted
* [HOME-DEVICE-RELAY-CONTRACTS.md](HOME-DEVICE-RELAY-CONTRACTS.md) is accepted
* controller vs relay app/package strategy is approved
* target device order is approved
* local-only first transport approach is approved
* prototype diagnostics expectations are approved

## Phase 14 Implementation Checklist

Before the first runtime commit:

* confirm `Phase 14D` is the active next runtime phase across README and docs
* confirm the AIO app direction is preserved
* confirm local-only first transport is approved
* confirm one Android phone or tablet is selected as the first relay target
* confirm one controller device is selected
* confirm one target PC already wakes by `Local Wi-Fi Wake`
* confirm placeholder-only test data is ready for pairing and wake-request validation
* confirm no Firebase, FCM, broker, or cloud work is being introduced in the first prototype
* confirm diagnostics fields required by the contract docs are visible in the first prototype scope
* confirm public wording still does not imply the relay path is already production-ready

## Recommended Next Safe Step

For the next runtime step after Phase 14C:

1. implement a local-only controller-to-relay prototype flow
2. keep the first relay target to a standard Android phone or tablet
3. keep transport local-only before any cloud or FCM work
4. add first real request/response handling without production auth or public transport
