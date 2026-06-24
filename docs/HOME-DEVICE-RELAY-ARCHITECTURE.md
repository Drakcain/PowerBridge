# Home Device Relay Architecture

## Status

`Home Device Relay` is not implemented in PowerBridge `v0.6.1`.

This document defines the intended architecture for a future relay path that helps normal users wake a home PC over cellular or other off-site networks without running a self-hosted server.

This is a design document only.

Phase status:

* `Phase 12` is complete as architecture
* `Phase 13` should validate contracts and prototype planning
* `Phase 14` should be the first runtime prototype phase

Non-goals for this phase:

* no runtime implementation
* no Firebase or FCM setup
* no cloud backend
* no public broker
* no relay pairing UI
* no Android foreground service code
* no TV-specific runtime work
* no smart plug integration
* no smart-home integration
* no Windows agent
* no shutdown, restart, or remote-control features

## Why This Exists

`Local Wi-Fi Wake` works when the controller phone is on the same LAN as the target PC.

`Home Relay Server` works for advanced users who already run a Raspberry Pi, NAS, Docker host, router-based relay, or similar home-side service.

Many users want something in between:

* easier than self-hosting a server
* still user-controlled
* still compatible with the real Wake-on-LAN network boundary

`Home Device Relay` is the intended answer for that gap.

## Core Network Boundary

A public cloud service cannot directly inject a Wake-on-LAN UDP Magic Packet into a private home LAN by itself.

The final Wake-on-LAN packet must be sent by something already inside the home network, such as:

* an always-on Android phone or tablet
* a Fire tablet
* a Fire TV or Android TV style device
* a Google TV device
* a user-owned relay server
* a router or VPN-based local anchor

This local-anchor requirement is mandatory and must remain visible in public docs and product wording.

## Core Actors

### Controller Phone

The main PowerBridge Android app used by the user.

Responsibilities:

* store PC profiles
* let the user choose a wake method
* initiate wake requests
* display relay state and diagnostics when available
* manage relay pairings later

### Home Relay Device

A separate always-on or mostly-always-on Android-based device left powered at home.

Examples:

* old Android phone
* Android tablet
* Fire tablet
* Fire TV
* Fire TV Cube
* Chromecast with Google TV
* Android TV / Google TV device
* other Android-based home device with local network access

Responsibilities:

* receive a wake request
* validate the request
* send the local UDP Magic Packet on the home LAN
* optionally return status or diagnostics

### Target PC

The Windows PC being woken.

Responsibilities:

* support Wake-on-LAN
* remain correctly configured for the desired wake state
* expose the needed local profile values, such as MAC address and broadcast path

### Optional Broker / Push Layer

Not implemented in this phase.

Future role:

* wake the relay device indirectly
* deliver minimum wake-event metadata
* never act as the final Wake-on-LAN sender into the private LAN

## Deployment Categories

### Legacy Mobile

Target examples:

* old Android phones
* Android tablets
* Pixel devices
* Galaxy devices
* Fire tablets where sideloading is practical

Strengths:

* easy to leave on power
* built-in Wi-Fi
* local network access is natural
* cameras and QR flows are easy if pairing is relay-side

Weaknesses:

* battery optimization and Doze can interfere
* OEM background rules vary widely
* some devices may sleep aggressively unless configured carefully

### Media Client

Target examples:

* Fire TV
* Fire TV Cube
* Chromecast with Google TV
* Android TV
* Google TV
* TV-box style Android devices

Strengths:

* often already always plugged in
* usually already connected to the TV/home network
* can be a convenient home-side anchor

Weaknesses:

* sideloading may be required
* background execution support varies
* foreground service UX may be awkward on TV
* remote navigation and pairing UX need separate design
* reliability is not yet proven across devices

### Persistent Server Compatibility

This phase does not implement server bridging.

The architecture should remain compatible with future use of:

* Raspberry Pi
* NAS
* Linux host
* Docker host
* router-based relay

This keeps `Home Device Relay` and `Home Relay Server` aligned instead of competing with each other.

## Recommended Product Shape

Recommended direction:

* keep the controller phone experience in the existing PowerBridge app
* design the relay runtime as a separate Android app or package later
* keep contracts shared across controller and relay components

Why:

* avoids bloating the normal controller app too early
* lets relay-specific permissions and background behavior stay isolated
* makes TV or tablet relay UX easier to specialize later
* keeps the current controller app simpler for normal users

Alternative future options:

* relay mode inside the main app
* both a standalone relay app and an in-app relay mode sharing the same contract

Initial recommendation:

* separate relay app/package first
* shared contracts
* no implementation in this phase

## High-Level Flow

Future intended flow:

1. User taps `Wake` on the controller phone.
2. Controller chooses the paired relay for the active PC profile.
3. Controller sends a secure wake request through a future transport.
4. Home relay device receives or polls for that request.
5. Home relay validates the request and the allowed target profile.
6. Home relay sends the local Wake-on-LAN UDP Magic Packet.
7. Home relay records result details and returns status when possible.

## Capability Model

Each future relay device record should be able to describe whether the device:

* can run a foreground activity
* can run a foreground service
* can receive push or event notifications
* can stay on external power
* can access the local network reliably
* can send UDP broadcast packets
* can report diagnostics
* may be battery-constrained
* may be background-restricted
* may lack Play Services
* may require sideloading

This should be treated as capability data, not a promise that all devices behave the same way.

## Pairing Model Draft

Future pairing should support these steps:

1. Controller creates a pairing request.
2. Relay device scans a QR code or enters a pairing code.
3. Relay device registers itself as a home relay candidate.
4. Controller links the relay to one or more PC profiles.
5. User can revoke the relay pairing later.
6. User can rotate pairing material later.

Pairing principles:

* pairing must not expose relay tokens in screenshots by default
* pairing must not include OS credentials
* pairing must not include PC passwords
* pairing should support one relay serving multiple profiles
* pairing should support one profile having multiple eligible relays later

Suggested contract names:

* `RelayPairingContract`
* `HomeRelayDevice`
* `RelayDeviceRegistry`

## Wake Request Model Draft

Suggested future wake-request fields:

* `profileId`
* `relayDeviceId`
* `targetMac`
* `broadcastIp`
* `udpPort`
* `requestId`
* `timestamp`
* `nonce`
* `auth` or signed-token concept

Rules:

* no Windows passwords
* no OS credentials
* no remote-desktop credentials
* no raw unauthenticated public wake endpoint
* only allow wake for configured profiles

Suggested contract names:

* `RelayWakeRequest`
* `RelayWakeResult`
* `RelayTransport`
* `PushRelayTransport`
* `LocalRelayTransport`

## Relay Diagnostics Model Draft

Suggested future relay-diagnostics fields:

* relay online or offline state
* last seen timestamp
* last wake request received
* last local Wake-on-LAN send attempt
* last selected network info
* battery or power state if available
* Wi-Fi or LAN status
* background restriction state
* push delivery status if available

Suggested contract name:

* `RelayDiagnostics`

These diagnostics should help the user answer:

* did the controller send a request
* did the relay receive it
* was the relay online
* was local network send attempted
* what limitation likely blocked success

## Security Model

Security principles:

* relay must validate requests before sending Wake-on-LAN
* no raw unauthenticated public wake endpoint
* no public-facing direct WOL packet injection from cloud
* no OS credentials stored for wake
* no PC passwords
* no silent remote-control capability
* no shutdown or restart scope in the relay contract
* only allow configured-profile wake targets
* pairing must be revocable
* pairing material should be rotatable

Security posture for later phases:

* minimum data over any broker path
* short-lived request identity where practical
* explicit relay-to-profile authorization
* diagnostics visible to the user, not silently exported

## Privacy Model

Privacy principles:

* no analytics by default
* no auto-upload diagnostics
* local network data stays user-controlled
* diagnostics are shared only by explicit user action
* any future broker should carry only minimum request data
* screenshots, QR codes, and logs should avoid exposing relay secrets

## Transport Options and Tradeoffs

### Future Push/Broker Model

Possible shape:

* controller sends a wake event through a minimal broker
* broker wakes or notifies the relay
* relay performs the final local WOL send

Pros:

* best chance at usable off-site wake UX
* reduces relay polling pressure

Cons:

* needs backend design later
* needs request authentication design
* may depend on Play Services availability on some devices

### Polling Fallback

Possible shape:

* relay checks for queued requests periodically

Pros:

* can work where push is weak or unavailable

Cons:

* worse latency
* higher battery/network cost
* less elegant UX

### Local-Only Relay Mode

Possible shape:

* controller and relay communicate only on the same LAN

Pros:

* simpler first contract
* useful for staged testing later

Cons:

* does not solve cellular wake by itself

### Self-Hosted Broker Mode

Possible shape:

* advanced users host their own wake-event broker later

Pros:

* aligns with existing user-owned infrastructure model

Cons:

* too complex for the first relay prototype

No transport is implemented in this phase.

## Android Platform Constraints

The architecture must account for:

* Doze
* app standby
* battery optimization
* foreground-service requirements
* notification requirements for long-running foreground work
* background execution limits
* Play Services availability differences
* Fire OS limitations
* Android TV background behavior differences
* local-network and UDP broadcast permission requirements later

Important truth:

An always-powered Android device is not automatically a reliable always-awake background relay.

## Fire TV / Android TV Constraints

The architecture must assume:

* sideloading may be required
* background execution varies by device and vendor
* foreground service UX on TV may be awkward
* remote-control navigation needs its own future UX pass
* always-powered does not guarantee always-awake relay behavior
* local UDP broadcast behavior must be tested on real hardware

Public wording should describe Fire TV, Android TV, and Google TV support as planned and experimental until real device testing proves otherwise.

## User-Facing Warnings

Future user-facing warnings should include:

* `Home Device Relay` requires a device left powered on at home
* reliability varies by device, OS version, and vendor policy
* Fire TV / Android TV support is experimental until tested
* off-site wake still depends on the relay being online
* wake from shutdown still depends on PC firmware and hardware support

## Relationship to Other PowerBridge Methods

### Home Device Relay

Future user-friendly relay path using an always-on Android-based home device.

### Home Relay Server

Live advanced path using user-owned infrastructure such as a NAS, Pi, Docker host, or server.

### Smart Plug Boot Assist

Future hardware-assisted boot path for systems that do not wake reliably through normal WOL.

### Smart Home Wake

Future research area for voice ecosystem or smart-home platform integrations where platform support allows.

The methods must remain distinct in docs and UI.

## Phase 14 Prototype Entry Criteria

Before Phase 14 implementation starts:

* this architecture is reviewed
* relay app versus app-mode choice is explicitly approved
* pairing contract draft is accepted
* wake request contract draft is accepted
* diagnostics contract draft is accepted
* minimum target test devices are selected
* security model is accepted
* expected user-facing warnings are accepted

Recommended minimum prototype targets:

* one standard Android phone or tablet
* one Fire tablet or Fire TV class device
* one Android TV or Google TV class device if available

## Recommended Next Step

Phase 13 should be a narrow prototype-planning and contract-validation phase, not a broad product build.

The first prototype should prove:

* one relay device can be paired safely
* one target PC profile can be authorized
* one local Wake-on-LAN send can be triggered through a relay-side path
* diagnostics are clear enough to explain failure modes
