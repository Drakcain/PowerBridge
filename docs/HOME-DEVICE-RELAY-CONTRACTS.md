# Home Device Relay Contracts

## Purpose

This document defines the draft contract layer for the future `Home Device Relay` path in PowerBridge.

These contracts exist to make Phase 14 implementation controlled, testable, and public-safe before any runtime relay code is written.

## Status

These are design-level draft contracts only.

They are not implemented runtime APIs.

## Non-Goals

This phase does not add:

* relay runtime implementation
* Firebase
* FCM
* cloud broker code
* Android foreground service code
* pairing UI
* network endpoints
* secrets
* production authentication plumbing

## Versioning Approach

Each relay contract should use an explicit schema name so controller and relay builds can validate shape safely.

Recommended initial schemas:

* `powerbridge.relay_pairing.v1`
* `powerbridge.relay_registration.v1`
* `powerbridge.relay_wake_request.v1`
* `powerbridge.relay_wake_result.v1`
* `powerbridge.relay_diagnostics.v1`
* `powerbridge.relay_capabilities.v1`

Rules:

* do not infer schema from app version
* change schema version only when compatibility actually changes
* allow later app versions to still understand older contract versions when practical

## Actor Model

### Controller Phone

The main PowerBridge Android app.

Responsibilities:

* initiate pairing
* choose target PC profile
* create wake requests
* display relay state and results

### Relay Device

A future home-side Android-based device left on power and on the home network.

Responsibilities:

* register itself
* validate linked profiles
* receive or fetch requests
* send local Wake-on-LAN packets
* report capabilities and diagnostics

### Target PC

The PC being woken through a linked PowerBridge profile.

### Optional Broker

Not implemented in this phase.

If added later, it should carry minimum request data and never perform the final LAN-side WOL send itself.

## Pairing Contract

Schema:

```text
powerbridge.relay_pairing.v1
```

Draft fields:

* `schema`
* `pairingRequestId`
* `controllerDeviceId`
* `controllerDisplayName`
* `createdAt`
* `expiresAt`
* `requestedPermissions`
* `pairingNonce`
* `pairingPublicKey`
* `pairingKeyHint`
* `relayGroupName`
* `profileLinkMode`

Field intent:

* `pairingRequestId` identifies the pairing attempt
* `controllerDeviceId` identifies the initiating controller app install/device identity
* `controllerDisplayName` is user-facing only
* `expiresAt` limits replay and stale pairing use
* `requestedPermissions` documents what the relay is expected to do later
* `pairingNonce` supports replay protection
* `pairingPublicKey` or `pairingKeyHint` supports future signed-request trust
* `relayGroupName` helps users label a relay group later
* `profileLinkMode` defines whether pairing starts empty, single-profile, or multi-profile

Placeholder example:

```json
{
  "schema": "powerbridge.relay_pairing.v1",
  "pairingRequestId": "pair_20260624_demo_001",
  "controllerDeviceId": "controller_demo_alpha",
  "controllerDisplayName": "Primary Phone",
  "createdAt": "2026-06-24T20:00:00Z",
  "expiresAt": "2026-06-24T20:10:00Z",
  "requestedPermissions": [
    "wake_linked_profiles",
    "report_relay_diagnostics"
  ],
  "pairingNonce": "nonce_demo_001",
  "pairingKeyHint": "controller-key-v1",
  "relayGroupName": "Home Relay Devices",
  "profileLinkMode": "manual_after_pairing"
}
```

## Relay Registration Contract

Schema:

```text
powerbridge.relay_registration.v1
```

Draft fields:

* `schema`
* `relayDeviceId`
* `relayDisplayName`
* `relayDeviceType`
* `appVersion`
* `platform`
* `osVersion`
* `manufacturer`
* `model`
* `capabilities`
* `registeredAt`
* `lastSeenAt`
* `relayPublicKey`
* `keyId`
* `linkedProfileIds`

Field intent:

* `relayDeviceId` is the stable relay identity
* `relayDisplayName` is user-facing only
* `relayDeviceType` supports later categories such as `android_phone`, `android_tablet`, `fire_tablet`, `fire_tv`, `android_tv`
* `capabilities` can embed or reference the capability report contract
* `linkedProfileIds` limits wake authorization to explicit profiles

Placeholder example:

```json
{
  "schema": "powerbridge.relay_registration.v1",
  "relayDeviceId": "relay_demo_alpha",
  "relayDisplayName": "Living Room Tablet",
  "relayDeviceType": "android_tablet",
  "appVersion": "0.0.0-prototype",
  "platform": "android",
  "osVersion": "14",
  "manufacturer": "Generic",
  "model": "Demo Tablet",
  "capabilities": {
    "schema": "powerbridge.relay_capabilities.v1",
    "relayDeviceId": "relay_demo_alpha",
    "canReceivePush": false,
    "canPoll": true,
    "canRunForegroundService": true,
    "canStayAwakeOnPower": true,
    "canAccessLocalNetwork": true,
    "canSendUdpBroadcast": true,
    "hasPlayServices": true,
    "requiresSideloading": false,
    "supportsTvUi": false,
    "batteryConstrained": false,
    "backgroundRestricted": false,
    "notes": "Placeholder capability report only."
  },
  "registeredAt": "2026-06-24T20:05:00Z",
  "lastSeenAt": "2026-06-24T20:05:00Z",
  "keyId": "relay-key-v1",
  "linkedProfileIds": [
    "profile_demo_gaming_pc"
  ]
}
```

## Wake Request Contract

Schema:

```text
powerbridge.relay_wake_request.v1
```

Draft fields:

* `schema`
* `requestId`
* `createdAt`
* `expiresAt`
* `controllerDeviceId`
* `relayDeviceId`
* `profileId`
* `targetDisplayName`
* `targetMac`
* `broadcastIp`
* `udpPort`
* `packetCount`
* `packetIntervalMs`
* `nonce`
* `authSignature`
* `authTokenRef`

Field intent:

* `requestId` tracks one wake attempt end-to-end
* `expiresAt` limits stale wake replay
* `targetDisplayName` is user-facing only
* `packetCount` and `packetIntervalMs` allow controlled packet behavior later
* `authSignature` or `authTokenRef` supports later signed or broker-backed authorization

Placeholder example:

```json
{
  "schema": "powerbridge.relay_wake_request.v1",
  "requestId": "wake_20260624_demo_001",
  "createdAt": "2026-06-24T20:15:00Z",
  "expiresAt": "2026-06-24T20:16:00Z",
  "controllerDeviceId": "controller_demo_alpha",
  "relayDeviceId": "relay_demo_alpha",
  "profileId": "profile_demo_gaming_pc",
  "targetDisplayName": "Gaming PC",
  "targetMac": "AA:BB:CC:DD:EE:FF",
  "broadcastIp": "192.168.1.255",
  "udpPort": 9,
  "packetCount": 3,
  "packetIntervalMs": 150,
  "nonce": "nonce_demo_wake_001",
  "authTokenRef": "auth-ref-demo-001"
}
```

## Wake Result Contract

Schema:

```text
powerbridge.relay_wake_result.v1
```

Draft fields:

* `schema`
* `requestId`
* `relayDeviceId`
* `profileId`
* `accepted`
* `sent`
* `packetCount`
* `broadcastIp`
* `udpPort`
* `startedAt`
* `completedAt`
* `resultCode`
* `message`
* `errorCategory`

Field intent:

* `accepted` means the relay accepted the request for processing
* `sent` means the relay attempted local WOL packet send
* `resultCode` should be short and machine-readable
* `message` can be user-facing
* `errorCategory` should group issues such as authorization, network, expiry, capability, or internal failure

Placeholder example:

```json
{
  "schema": "powerbridge.relay_wake_result.v1",
  "requestId": "wake_20260624_demo_001",
  "relayDeviceId": "relay_demo_alpha",
  "profileId": "profile_demo_gaming_pc",
  "accepted": true,
  "sent": true,
  "packetCount": 3,
  "broadcastIp": "192.168.1.255",
  "udpPort": 9,
  "startedAt": "2026-06-24T20:15:02Z",
  "completedAt": "2026-06-24T20:15:03Z",
  "resultCode": "wol_sent",
  "message": "Relay sent the local Wake-on-LAN packet sequence.",
  "errorCategory": "none"
}
```

## Relay Diagnostics Contract

Schema:

```text
powerbridge.relay_diagnostics.v1
```

Draft fields:

* `schema`
* `relayDeviceId`
* `relayDisplayName`
* `relayDeviceType`
* `appVersion`
* `platform`
* `osVersion`
* `manufacturer`
* `model`
* `networkType`
* `localIp`
* `gateway`
* `subnetPrefix`
* `broadcastIp`
* `canSendUdpBroadcast`
* `batteryPercent`
* `powerConnected`
* `batteryOptimizationState`
* `foregroundServiceState`
* `lastSeenAt`
* `lastWakeRequestAt`
* `lastWakeResult`
* `lastError`

Field intent:

* this contract helps explain why a wake attempt did or did not succeed
* `lastWakeResult` can be a compact embedded summary
* `lastError` should stay machine-readable where possible

Placeholder example:

```json
{
  "schema": "powerbridge.relay_diagnostics.v1",
  "relayDeviceId": "relay_demo_alpha",
  "relayDisplayName": "Living Room Tablet",
  "relayDeviceType": "android_tablet",
  "appVersion": "0.0.0-prototype",
  "platform": "android",
  "osVersion": "14",
  "manufacturer": "Generic",
  "model": "Demo Tablet",
  "networkType": "wifi",
  "localIp": "192.168.1.50",
  "gateway": "192.168.1.1",
  "subnetPrefix": 24,
  "broadcastIp": "192.168.1.255",
  "canSendUdpBroadcast": true,
  "batteryPercent": 100,
  "powerConnected": true,
  "batteryOptimizationState": "unknown",
  "foregroundServiceState": "not_running",
  "lastSeenAt": "2026-06-24T20:18:00Z",
  "lastWakeRequestAt": "2026-06-24T20:15:00Z",
  "lastWakeResult": "wol_sent",
  "lastError": ""
}
```

## Relay Capability Contract

Schema:

```text
powerbridge.relay_capabilities.v1
```

Draft fields:

* `schema`
* `relayDeviceId`
* `canReceivePush`
* `canPoll`
* `canRunForegroundService`
* `canStayAwakeOnPower`
* `canAccessLocalNetwork`
* `canSendUdpBroadcast`
* `hasPlayServices`
* `requiresSideloading`
* `supportsTvUi`
* `batteryConstrained`
* `backgroundRestricted`
* `notes`

Placeholder example:

```json
{
  "schema": "powerbridge.relay_capabilities.v1",
  "relayDeviceId": "relay_demo_alpha",
  "canReceivePush": false,
  "canPoll": true,
  "canRunForegroundService": true,
  "canStayAwakeOnPower": true,
  "canAccessLocalNetwork": true,
  "canSendUdpBroadcast": true,
  "hasPlayServices": true,
  "requiresSideloading": false,
  "supportsTvUi": false,
  "batteryConstrained": false,
  "backgroundRestricted": false,
  "notes": "Placeholder values only."
}
```

## Security Rules

All relay contracts should follow these rules:

* use request expiration
* use nonce-based replay protection
* use signed requests or short-lived token references later
* relay validates linked profile authorization before any WOL send
* relay only wakes profiles explicitly linked to that relay
* pairing must be revocable
* key or token rotation must remain possible later
* no unauthenticated public wake endpoint
* no raw WOL endpoint exposed to the internet
* any future broker carries only minimum required request data

## Privacy Rules

All relay contracts should follow these rules:

* no analytics by default
* no auto-upload diagnostics
* diagnostics shared only by user action
* local network data stays user-controlled
* no OS credentials in broker traffic
* no PC passwords in broker traffic
* minimize target identifiers where possible

## Forbidden Fields

These fields must not appear in relay contracts, public examples, screenshots, or docs:

* `username`
* `password`
* `adminPassword`
* `token`
* `relayToken`
* `apiKey`
* `secret`
* `credential`
* `privateKey`
* `sshKey`
* `winrmPassword`
* `rdpPassword`
* Windows login data
* PC login data

If an implementation later needs secret-bearing transport data, it should be stored and transported through a separate protected mechanism, not raw public-facing docs/examples.

## Schema Evolution Notes

Future changes should prefer:

* additive fields where possible
* explicit schema-version bumps when compatibility changes
* tolerant parsing for unknown optional fields
* machine-readable enums for result and error categories
* separate contract docs for runtime transport once implementation begins
