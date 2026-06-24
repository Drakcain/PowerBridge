# Security Policy

## Reporting a Vulnerability

If you discover a security issue in PowerBridge, do not post sensitive details publicly in an issue, discussion, screenshot, or release note.

When reporting:

* describe the affected component
* describe the impact
* provide reproduction steps
* redact private IP addresses, MAC addresses, tokens, secrets, and local user paths

If the issue involves generated diagnostics, QR payloads, or logs, sanitize them before sharing.

## Sensitive Data Rules

Do not include any of the following in public reports:

* relay tokens
* passwords
* API keys
* local absolute user paths
* private IP addresses that identify your environment
* private MAC addresses
* generated QR payloads with live values
* diagnostics ZIPs containing live values

## Supported Versions

Early public support policy placeholder:

* current public version line: supported
* older pre-public local builds: unsupported

This policy may be refined as formal releases begin.

## Responsible Disclosure

Please allow time for triage and remediation before publishing exploit details.

PowerBridge does not intentionally collect credentials, secrets, or analytics by default. Reports that identify a leak path for local network values, diagnostics, QR payloads, or user-owned relay data are especially important.
