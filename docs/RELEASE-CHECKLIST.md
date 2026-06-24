# Release Checklist

Before a public push or release prep checkpoint:

## Build and Validation

* build Android debug
* build Android release
* run Windows Companion validation
* run Windows Companion CLI smoke test

## Sanitization

* run the public-value sweep
* confirm no generated diagnostics ZIPs are staged
* confirm no generated QR or JSON output is staged
* confirm no local screenshots with live values are staged
* confirm no keystores or signing files are staged
* confirm no local properties are staged

## Documentation

* confirm root `README.md` is accurate
* confirm `android/README.md` is accurate
* confirm `windows-companion/README.md` is accurate
* confirm `BUILD.md` is accurate
* confirm `SECURITY.md` is present
* confirm `SIGNING.md` is present
* confirm `THIRD-PARTY-NOTICES.md` is present
* confirm privacy and sanitization docs are present

## Versioning

* confirm `VERSION` matches the intended public version
* confirm Android `versionName` matches the intended public version
* confirm release notes do not claim unimplemented features

## Release Hygiene

* do not push until final review is complete
* do not create a GitHub release until explicitly requested
* do not tag until explicitly requested
* generate checksums later when real release artifacts exist
* publish the Android APK and Windows Companion installer as separate top-level release assets
* do not ship the release page as a nested folder/archive layout unless technically required
* confirm planned release asset names are clear and user-readable
* for PowerBridge Android, publish a locally signed or locally stable-installable APK from this workstation until a dedicated release-signing path exists
