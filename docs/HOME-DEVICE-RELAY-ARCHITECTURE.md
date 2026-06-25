# Old Android Relay Architecture

PowerBridge remote wake is now scoped to three approved remote method families:

1. `Old Android Device`
2. `Alexa / Google Voice Devices`
3. `My Own Server`

This architecture document covers the first runtime target: `Old Android Device`.

## Architecture Shape

```text
Controller phone
  -> secure wake request
Old Android relay device at home
  -> local Wake-on-LAN packet
Target PC
```

The target PC does not need to run a background agent while sleeping or powered off.

## Why This Exists

A phone on cellular cannot directly broadcast a Wake-on-LAN packet into a private home LAN.

Something inside the home network must receive the request and send the local packet.

For normal users, an old Android phone or tablet is the simplest hardware anchor to explain:

```text
Leave this old device plugged in at home.
It wakes your PC when your main phone asks.
```

## Trust Boundary

The relay must not expose a raw public wake endpoint.

Required design rules:

* pair the relay to specific profiles
* reject unknown requests
* expire wake requests
* never store PC passwords
* never collect OS credentials
* keep relay status visible to the user

## Runtime Requirements

The old Android relay path needs:

* pairing payload
* relay device identity
* target profile identity
* target MAC and broadcast IP
* request expiration
* local packet send result
* diagnostics event log

## User Experience Requirement

The setup should remain direct:

```text
Open Setup Guide
Scan Windows Companion QR
Confirm PC
Choose Old Android Device
Pair the old Android relay
Leave relay plugged in
Wake from main phone
```

No normal user should need to type:

* subnet
* broadcast IP
* gateway
* token
* relay URL

Those values can exist in diagnostics and advanced screens, but not as the normal path.
