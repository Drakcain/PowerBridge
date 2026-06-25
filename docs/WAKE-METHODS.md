# PowerBridge Wake Methods

PowerBridge has one simple user goal:

```text
Choose how this PC should wake, then press Wake.
```

The app must hide networking details from normal users while staying honest about which methods are live.

## Current Method Truth

| Method | User Label | Current Status | What It Needs |
| --- | --- | --- | --- |
| Same home network | Local Wi-Fi Wake | Live | Phone and PC on the same Wi-Fi/LAN, valid target MAC, valid broadcast IP |
| Persistent server | My Own Server | Live for advanced users | User-owned relay/server/NAS/Pi/router/Docker host reachable by the Android app |
| Legacy mobile | Old Phone / Tablet Relay | Prototype | A relay app on a spare Android device, visible/foreground reliability model, secure pairing |
| Media client | Fire TV / Smart TV Relay | Planned | TV-friendly relay app/runtime on a supported Android TV, Google TV, or Fire TV class device |
| Voice ecosystem | Smart Speaker Wake | Research | Official smart-home integration path, account linking, local or cloud fulfillment |
| Hardware bypass | Smart Plug Power-On | Planned | Compatible smart plug plus BIOS/UEFI restore-on-AC-power behavior |

## v1.0 Rule

`v1.0.0` does not require every method to be fully implemented.

`v1.0.0` requires:

* live methods work reliably
* planned methods are clearly labeled
* no UI implies a method works before it really works
* non-technical users can complete the guided setup without understanding WOL internals

## User-Friendly Flow

The target flow remains:

```text
Install PowerBridge
Run Windows Companion
Scan QR
Confirm PC
Choose where Wake should work
Choose the simplest available method
Wake / Boot from the main screen
```

## Method Implementation Notes

### Local Wi-Fi Wake

This is the required core path. It sends a Wake-on-LAN Magic Packet from the Android phone on the same home network.

Keep this path simple:

* scan the Windows Companion QR
* confirm the PC profile
* use the imported MAC and broadcast IP
* show clear failure text if the phone is not on the home network

### My Own Server

This is the current advanced off-site path. PowerBridge sends a request to a user-owned relay/server, and that relay sends the local wake packet from inside the home network.

Do not present this as beginner setup unless PowerBridge also ships the server installer and pairing flow.

### Old Phone / Tablet Relay

This is the most promising beginner remote-wake path, but it should not be treated as live until the relay runtime exists.

The likely product shape is:

```text
Main phone app
Spare Android relay app or relay mode
Secure QR pairing
Visible foreground notification on the spare device
FCM or another broker path for remote trigger
Local WOL packet sent by the spare device
```

Android background reliability is the hard part. Official Android guidance expects foreground services to show a notification, and Firebase high-priority messages are intended for time-sensitive delivery but still have platform behavior limits.

### Fire TV / Smart TV Relay

This is a separate method from the spare-phone path. It needs TV-specific runtime testing and should stay planned until a supported TV relay can receive a request and send the local wake packet reliably.

Do not assume every streaming device allows the same background behavior.

### Smart Speaker Wake

Alexa has an official Wake-on-LAN controller interface for smart-home devices that support WOL or WoWLAN. Google Home local fulfillment is also an official smart-home route for routing intents locally.

This is not just an Android button. It requires a real smart-home integration model, account/linking design, and certification or ecosystem-specific setup.

### Smart Plug Power-On

This is a hardware bypass, not Wake-on-LAN.

The simple user story is:

```text
Smart plug turns power off/on
PC motherboard is configured to power on when AC power returns
```

This must be documented carefully because power-cycling a PC can cause data loss if the PC is not already shut down.

## External References

* Android services overview: https://developer.android.com/develop/background-work/services
* Android foreground services overview: https://developer.android.com/develop/background-work/services/fgs
* Firebase Android message priority: https://firebase.google.com/docs/cloud-messaging/android-message-priority
* Alexa WakeOnLANController: https://developer.amazon.com/en-US/docs/alexa/device-apis/alexa-wakeonlancontroller.html
* Google Home local fulfillment: https://developers.home.google.com/local-home/overview

