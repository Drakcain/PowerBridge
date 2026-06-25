# Security Policy

## Reporting a Vulnerability

If you discover a security issue in PowerBridge, do **not** post sensitive details publicly in a GitHub issue, discussion, screenshot, release note, diagnostics ZIP, or public chat.

When reporting a vulnerability, include:

* the affected component
* the affected version or commit, if known
* the expected behavior
* the actual behavior
* the security impact
* reproduction steps
* any relevant logs or diagnostics after sanitization

Affected components may include:

* Android app
* Windows Companion
* setup QR / JSON payload handling
* diagnostics export
* profile storage
* relay configuration
* release packaging
* update or release scripts
* documentation that could cause unsafe setup or data exposure

## Sensitive Data Rules

Do not include live private values in public reports.

Redact or remove:

* relay tokens
* passwords
* API keys
* bearer tokens
* signing credentials
* keystores
* signing property files
* `local.properties`
* local absolute user paths
* private IP addresses
* private MAC addresses
* local hostnames that identify a private environment
* generated QR payloads with live values
* generated setup JSON payloads with live values
* diagnostics ZIPs containing live values
* logs containing local network details
* screenshots showing IP addresses, MAC addresses, tokens, endpoints, local paths, or QR payloads

If a screenshot, diagnostics file, QR image, JSON setup payload, or log is needed to explain the issue, sanitize it first.

## Generated Diagnostics and QR Payloads

PowerBridge diagnostics, QR payloads, setup JSON files, and logs may contain local network values.

These values can include:

* PC profile names
* MAC addresses
* local IP addresses
* broadcast addresses
* wake ports
* route settings
* relay endpoints
* readiness status
* local adapter details
* app configuration state

Treat these files as potentially sensitive.

Do not publish raw diagnostics ZIPs, QR images, setup JSON payloads, screenshots, or logs unless all live local values have been removed or replaced with safe examples.

## User-Owned Relay Configuration

PowerBridge does not ship with a shared relay service, shared token, or developer-managed wake infrastructure.

Remote wake paths are user-owned. Security reports involving relay behavior should avoid exposing:

* user-owned relay URLs
* authentication tokens
* home server details
* local network topology
* router or firewall details
* private endpoint names
* live request payloads

## Supported Versions

PowerBridge is currently in early public development.

| Version line                  | Support status                            |
| ----------------------------- | ----------------------------------------- |
| Latest public release         | Supported                                 |
| Current development branch    | Best-effort development support           |
| Older pre-public local builds | Unsupported                               |
| Private test builds           | Unsupported unless specifically requested |

This policy may be refined once formal public releases begin.

## Responsible Disclosure

Please allow reasonable time for triage and remediation before publishing exploit details.

Reports are especially important if they identify:

* leaked local network values
* leaked relay tokens or secrets
* unsafe diagnostics export behavior
* unsafe QR payload handling
* unsafe setup JSON handling
* unsafe release packaging
* accidental inclusion of keystores or signing material
* accidental publication of generated artifacts
* update or release script behavior that could publish unsafe artifacts
* documentation that could cause users to expose private setup values

## Security Expectations

PowerBridge should not intentionally collect:

* passwords
* operating-system credentials
* Windows admin credentials
* router credentials
* BIOS or UEFI credentials
* analytics by default
* private relay secrets for developer-managed infrastructure

PowerBridge should not implement:

* remote shutdown
* remote restart
* remote command execution
* remote desktop control
* SSH / WinRM / RPC control paths
* shared public relay credentials
* hidden developer relay defaults

## Public Example Data

When examples are needed, use safe placeholder values.

Recommended examples:

```text
PC name: Gaming PC
MAC address: AA:BB:CC:DD:EE:FF
Local IP: 192.168.1.100
Broadcast IP: 192.168.1.255
Wake port: 9
Relay URL: https://example.com/powerbridge
Token: example-token-redacted
```

Do not use real user values in examples, screenshots, documentation, issue templates, release notes, or test payloads.

## Release and Repository Safety

Before publishing a release or committing release-related changes, verify that the repository does not include:

* generated diagnostics ZIPs
* generated QR images with live values
* generated setup JSON files with live values
* logs with local network details
* screenshots with real values
* APKs, AABs, EXEs, ZIPs, or checksums unless they are intentional release assets
* signing keys
* keystores
* signing properties
* `local.properties`
* passwords
* tokens
* private endpoint values

Run the public-value sweep described in `BUILD.md` before publishing a public release.

## No Warranty

PowerBridge is provided without warranty. Wake behavior and remote wake behavior depend on user-owned hardware, firmware, operating-system configuration, network topology, and user-managed relay paths.
