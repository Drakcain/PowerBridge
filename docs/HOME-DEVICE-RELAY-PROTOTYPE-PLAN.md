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

## Phase 14C.1 Status

Phase 14C.1 was a focused polish pass after the guided setup wiring work.

Completed in Phase 14C.1:

* verified the final guided setup confirm path stays intact
* added `Share Report` to `Home Relay Mode`
* reduced noisy CI runs with Android/Windows workflow path filters
* aligned development versioning to the phase-level 0.7.x rule

Not implemented in Phase 14C.1:

* old phone or tablet relay runtime
* Firebase / FCM
* cloud broker
* smart plug runtime
* smart speaker runtime
* TV runtime

## Phase 14D Goal

Phase 14D is a roadmap and product-follow-through phase centered on the main Android APK experience before deeper relay runtime work resumes.

The Phase 14D goal is:

* make the current method families easier for normal users to understand
* tighten readiness guidance around what is live now versus what is still planned
* improve the `My Own Server` path clarity for advanced users
* add guide-first direction for `Smart Plug Power-On`, `Smart Speaker Wake`, and `Fire TV / Smart TV Relay`
* keep old-phone relay runtime paused until the product shape is clearer

## Deferred Runtime Order

When deeper relay runtime resumes later:

1. standard Android phone or tablet first
2. Fire tablet second
3. Fire TV / Android TV / Google TV later

Reason:

* normal Android devices are easier to test and debug first
* Fire tablets are useful but may add sideloading and platform-policy complexity
* TV-class devices should come after the basic relay contract and flow are proven

## Phase 14D Method-Guidance Focus

Recommended Phase 14D order:

1. `My Own Server`
2. `Smart Plug Power-On`
3. `Smart Speaker Wake`
4. `Fire TV / Smart TV Relay`
5. keep `Old Phone / Tablet Relay` paused

Important boundary:

* keep this phase in the main APK guidance/readiness layer
* do not jump into relay runtime, cloud, or TV implementation during this pass

## Deferred Runtime Requirements

When runtime relay implementation resumes later, the minimum test environment should still be:

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

Recommended continuing direction:

* keep the PowerBridge Android app as the single user-facing Android app
* keep relay behavior inside the same app under a clearly marked `Home Relay Mode`
* keep the Windows Companion separate

Why:

* preserves the intended one-APK Android product shape
* still keeps runtime scope narrow
* keeps relay diagnostics and placeholder transport visible without creating a second end-user app

## Package / App Strategy

Recommended continuing package strategy:

* controller and relay prototype both live in the existing PowerBridge Android app
* contracts remain documented first, then can become shared Kotlin models inside the app as runtime work grows

## Feature Flags / Staged UI Guidance

Phase 14D should keep UI scope narrow.

Recommended guidance:

* hide unfinished relay features behind internal prototype toggles if needed
* do not expose a public-facing “fully works over cellular” promise
* keep pairing/testing surfaces obviously experimental until validated

## Diagnostics Requirements

For later runtime work, diagnostics should eventually answer:

* did the controller create a request
* did the relay receive it
* was the request authorized
* was the request expired
* did the relay attempt to send WOL
* what broadcast IP and UDP port were used
* what device/network limitation likely blocked success

At minimum, later runtime diagnostics should record:

* request id
* relay device id
* profile id
* timestamps
* result code
* error category
* network state summary
* battery/background state summary

## Security Requirements

Phase 14D and later work must still respect the design boundaries:

* no raw unauthenticated public wake endpoint
* no PC passwords
* no OS credentials
* no long-lived secret examples in docs
* relay only wakes linked profiles
* request expiration and nonce support remain part of the design

Prototype shortcuts are acceptable only if they stay local-only, explicit, and non-public-facing.

## Platform Risks

Key deferred runtime risks:

* Doze and battery optimization may suppress background behavior
* OEM background policies may differ significantly
* Fire OS behavior may differ from standard Android
* TV navigation and TV foreground UX may be awkward
* always-on power does not guarantee the device remains ready to receive work
* UDP broadcast behavior must be validated on real hardware

## What Is Explicitly Not Built Yet

Phase 14D should not include:

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

## Success Criteria For Phase 14D

Phase 14D should be considered successful only if it delivers:

* clearer consumer-facing method guidance in the main APK
* better readiness truth for live versus planned paths
* stronger advanced-path guidance for `My Own Server`
* practical guide-first framing for smart plug, smart speaker, and TV-class paths
* no false claim that remote relay runtime is already complete

## Phase 14D Entry Criteria

Before Phase 14D implementation starts:

* [HOME-DEVICE-RELAY-ARCHITECTURE.md](HOME-DEVICE-RELAY-ARCHITECTURE.md) is accepted
* [HOME-DEVICE-RELAY-CONTRACTS.md](HOME-DEVICE-RELAY-CONTRACTS.md) is accepted
* controller vs relay app/package strategy is approved
* method-guidance-first direction is approved
* deferred runtime order is accepted
* consumer setup workflow remains the top priority

## Phase 14D Implementation Checklist

Before the first Phase 14D commit:

* confirm `Phase 14D` is the active next runtime phase across README and docs
* confirm the AIO app direction is preserved
* confirm local-only first transport is approved
* confirm `My Own Server` remains the only advanced live remote direction
* confirm smart plug / smart speaker / TV guidance stays non-runtime and non-live
* confirm old phone / tablet relay remains paused
* confirm no Firebase, FCM, broker, or cloud work is introduced
* confirm public wording still does not imply the relay path is already production-ready

## Recommended Next Safe Step

For the next implementation step after this roadmap realignment:

1. improve `My Own Server` method guidance and readiness explanation inside the main APK
2. add checklist-first guidance for `Smart Plug Power-On`
3. add guide/research placeholders for `Smart Speaker Wake`
4. add guide/experimental explanation for `Fire TV / Smart TV Relay`
