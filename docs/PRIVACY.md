# Privacy

PowerBridge is designed to be local-first and user-controlled.

## Default Privacy Stance

By default, PowerBridge does not:

* include analytics
* auto-upload diagnostics
* auto-upload QR payloads
* collect Windows passwords
* collect relay tokens automatically

## Diagnostics

Diagnostics are generated locally. A report is shared only when the user explicitly chooses to share it.

Diagnostics may contain local setup information. Users should review and redact as needed before posting anything publicly.

## QR Setup Payloads

Windows Companion QR payloads contain local network setup values such as:

* PC name
* MAC address
* private IPv4 address
* gateway
* subnet prefix
* broadcast IP

These values are setup data, not secrets, but they are still environment-specific and should not be published casually.

## User-Owned Relay Data

If a user configures `Home Relay Server`, any relay URL, token, or related secret remains user-owned private configuration and should not be posted publicly.

## Public Safety Rule

Do not include live diagnostics ZIPs, live QR screenshots, or live setup JSON in public issues, docs, screenshots, or release assets unless intentionally sanitized.
