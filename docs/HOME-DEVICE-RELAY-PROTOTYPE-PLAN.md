# Home Device Relay Prototype Plan

## Phase 13 Status

Phase 13 is planning and contract validation only.

No relay runtime implementation is added in this phase.

## Phase 14 Prototype Goal

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

Recommended Phase 14 direction:

* build the prototype as a separate Android relay app/package or clearly isolated relay module
* keep the current PowerBridge controller app focused on controller responsibilities
* do not merge always-on relay behavior into the normal user app until the prototype proves it is viable

Why:

* cleaner trust and permission boundaries
* cleaner background-behavior testing
* easier to isolate relay diagnostics
* lower risk of bloating the existing controller app too early

## Package / App Strategy

Recommended package strategy:

* controller app remains the existing PowerBridge app
* relay prototype should live as a separate Android app/package
* contracts remain documented first, then can become shared Kotlin models later

Deferred decision:

* whether production PowerBridge eventually ships both a controller app and relay app, or later gains an optional relay mode

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

## Phase 14 Entry Criteria

Before implementation starts:

* [HOME-DEVICE-RELAY-ARCHITECTURE.md](HOME-DEVICE-RELAY-ARCHITECTURE.md) is accepted
* [HOME-DEVICE-RELAY-CONTRACTS.md](HOME-DEVICE-RELAY-CONTRACTS.md) is accepted
* controller vs relay app/package strategy is approved
* target device order is approved
* local-only first transport approach is approved
* prototype diagnostics expectations are approved

## Phase 14 Implementation Checklist

Before the first runtime commit:

* confirm `Phase 14` is the active next phase across README and docs
* confirm the separate relay app/package direction is approved
* confirm local-only first transport is approved
* confirm one Android phone or tablet is selected as the first relay target
* confirm one controller device is selected
* confirm one target PC already wakes by `Local Wi-Fi Wake`
* confirm placeholder-only test data is ready for pairing and wake-request validation
* confirm no Firebase, FCM, broker, or cloud work is being introduced in the first prototype
* confirm diagnostics fields required by the contract docs are visible in the first prototype scope
* confirm public wording still does not imply the relay path is already production-ready

## Recommended Next Safe Step

Before any code work:

1. approve the contract schemas
2. approve the separate relay app/package direction
3. approve the Phase 14 target-device order
4. approve local-only first transport for the prototype
