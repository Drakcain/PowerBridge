# Old Android Relay Prototype Plan

This plan replaces the earlier broad home-device relay exploration.

PowerBridge will not try to support every possible home device as a relay. The runtime prototype target is now:

```text
Old Android phone or tablet left plugged in at home
```

## Goal

Make remote wake understandable for non-technical users:

```text
Install PowerBridge
Run Windows Companion
Scan QR on main phone
Pair old Android device
Leave old Android device plugged in at home
Tap Wake from the main phone
Old Android device sends the local wake packet
```

## Minimum Prototype

The first prototype should include:

* one controller phone
* one old Android relay device
* one target PC already proven with `Local Wi-Fi Wake`
* QR pairing or equivalent local setup transfer
* visible relay state on the old Android device
* local Wake-on-LAN packet sent by the relay device
* diagnostics on controller and relay sides

## Not In Scope

Do not include these in the old-Android prototype:

* production cloud broker
* broad device ecosystem support
* silent background behavior
* shutdown/restart/remote desktop
* PC passwords or OS credentials
* unauthenticated public wake endpoints

## Reliability Rules

The relay device must make its state obvious to the user.

The app should explain:

* relay device must stay powered
* battery optimization may block reliability
* network path must remain available
* wake from shutdown still depends on PC hardware and firmware

## Success Criteria

The prototype is successful only when:

* the user can pair the old Android device without typing network values
* the relay can receive a wake request in a controlled test path
* the relay can send the local wake packet
* diagnostics identify whether pairing, request delivery, or local wake failed
* the UI still presents only the approved three remote method families
