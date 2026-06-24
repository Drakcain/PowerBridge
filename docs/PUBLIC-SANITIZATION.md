# Public Sanitization

This repository is intended for public release. Public-facing files must remain free of personal infrastructure and environment-specific leakage.

## Banned Public Content

Do not commit or publish:

* private IP addresses
* private MAC addresses
* private domains
* relay tokens
* passwords
* API keys
* local absolute user paths
* generated diagnostics ZIPs
* generated QR payloads with live values
* generated QR screenshots with live values
* personal setup wording

Public docs and source must not reference:

* legacy donor app names
* developer names or handles
* private donor-app wording
* developer-specific infrastructure wording

## Generated Artifact Exclusions

Keep these out of source control:

* Android build outputs
* Gradle caches
* Windows Companion generated output
* logs
* local screenshots
* signing keys
* keystores
* local properties

## Pre-Publish Sweep

Run from the repo root:

```powershell
rg -n --glob '!android/app/build/**' --glob '!android/build/**' --glob '!android/.gradle/**' --glob '!windows-companion/output/**' --glob '!**/*.apk' --glob '!**/*.aab' --glob '!**/*.zip' --glob '!**/*.png' --glob '!**/*.jpg' --glob '!**/*.jpeg' "(legacy donor app|developer handle|private relay assumption|relayToken|Bearer sk-|sk-proj-|sk-[A-Za-z0-9]{20,}|[A-Za-z]:\\\\Users\\\\|/Users/)" .
```

Any hit in active source, UI strings, defaults, comments, docs, examples, screenshots, or release assets should be treated as a publish blocker until reviewed.
