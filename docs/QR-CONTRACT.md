# QR Contract

PowerBridge uses a setup-only QR contract to transfer local network values from Windows Companion to the Android app.

Current schema:

```text
powerbridge.local_setup.v1
```

## Purpose

The QR contract prepares or updates an Android `Local Wi-Fi Wake` profile.

It is not:

* a wake command
* a relay command
* an authentication token
* a credential exchange
* a cloud pairing mechanism

## Required Safety Rules

The QR payload must remain secret-free.

Forbidden content includes:

* relay tokens
* passwords
* API keys
* private keys
* account credentials
* remote desktop credentials
* WinRM credentials
* SSH credentials

## Public Release Rule

Generated QR images and JSON payloads may contain real local network values. They must not be committed, included in release packages, or posted publicly without redaction.

Detailed contract documentation remains in:

* [windows-companion/docs/QR-CONTRACT.md](../windows-companion/docs/QR-CONTRACT.md)
