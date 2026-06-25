# Signing Notes

PowerBridge release signing is local-only in the current development phase.

This document defines how Android signing, Windows package signing, release artifact trust, and signing-material safety should be handled.

PowerBridge must never commit real signing keys, keystores, signing passwords, private certificates, or local machine paths.

## Android Release Signing Foundation

PowerBridge Android release signing is local-only for now.

Current rules:

* keep the real Android release keystore outside the repository
* keep real signing properties out of source control
* do not commit passwords
* do not commit private absolute paths
* do not commit keystores
* do not treat GitHub Actions validation builds as official signed release builds
* do not claim a build is signed unless signing was actually performed and verified

## Android Artifact States

PowerBridge currently has four distinct Android artifact states.

### Debug APK

Produced by:

```powershell
Set-Location "<repo-root>\android"
.\gradlew.bat :app:assembleDebug --no-daemon
```

Use:

* local development
* local testing
* install validation
* debugging

Do not use as:

* official production release
* final public Android artifact
* `v1.0.0` release asset

### Unsigned Release APK

Produced by:

```powershell
Set-Location "<repo-root>\android"
.\gradlew.bat :app:assembleRelease --no-daemon
```

This may occur when signing properties are absent or incomplete.

Use:

* CI validation
* release build validation
* packaging sanity checks

Do not use as:

* official signed public APK
* production release artifact
* proof that Android release signing is complete

### Locally Signed Release APK

Produced by `assembleRelease` when local signing properties are present and valid.

Use:

* future official GitHub release APK assets
* final public Android APKs
* release candidate validation

This is the intended Android artifact type for public releases.

### Future CI-Signed Release APK

Not implemented in this phase.

This would require:

* GitHub Actions secrets
* deliberate workflow changes
* protected release process
* key-handling policy
* audit of who can trigger signed releases

Do not assume CI-signed release support exists until it is explicitly implemented and documented.

## Local Signing File Layout

Use one of these ignored local files inside `android/`:

```text
android/signing.properties
android/keystore.properties
```

Use the tracked example as the template:

```text
android/signing.properties.example
```

Required keys:

```properties
storeFile=C:/Path/Outside/Repo/powerbridge-release.jks
storePassword=CHANGE_ME
keyAlias=powerbridge
keyPassword=CHANGE_ME
```

The real file must not be committed.

## Keystore Location

Store the real release keystore outside the repository.

Example private location:

```text
C:\Secure\Android\powerbridge-release.jks
```

Do not store the real keystore under:

```text
<repo-root>
<repo-root>\android
<repo-root>\dist
<repo-root>\assets
<repo-root>\docs
synced screenshots folders
generated release folders
```

The keystore should be backed up safely. Losing the release keystore can prevent future updates from being installed over earlier signed builds.

## Generate a Keystore

Example placeholder command:

```powershell
keytool -genkeypair -v -keystore "C:\Path\Outside\Repo\powerbridge-release.jks" -alias powerbridge -keyalg RSA -keysize 4096 -validity 10000
```

Replace the path with a private location outside the repository.

Do not paste real passwords into documentation, screenshots, issues, diagnostics, or release notes.

## Gradle Behavior

Current Gradle behavior should remain safe:

* if `android/signing.properties` or `android/keystore.properties` exists and is complete, `assembleRelease` signs the Android release build locally
* if signing properties are absent or incomplete, `assembleRelease` still runs without local signing
* debug builds continue to work without signing setup
* public GitHub Actions validation does not require private signing secrets
* unsigned release output is validation-only, not an official signed release artifact

## Local Build Commands

### Debug Build

```powershell
Set-Location "<repo-root>\android"
.\gradlew.bat :app:assembleDebug --no-daemon
```

### Release Build

```powershell
Set-Location "<repo-root>\android"
.\gradlew.bat clean :app:assembleRelease --no-daemon
```

## Verify the Signed APK

Use `apksigner` after a local signed release build:

```powershell
& "<android-sdk>\build-tools\34.0.0\apksigner.bat" verify --verbose ".\app\build\outputs\apk\release\app-release.apk"
```

If the local build is unsigned, official signing verification is not meaningful. Fix signing properties first.

## Generate SHA-256 Checksum

After producing the signed APK:

```powershell
Get-FileHash ".\app\build\outputs\apk\release\app-release.apk" -Algorithm SHA256
```

The checksum should match the exact APK that will be published.

## Release Script Truth

`scripts/Build-PowerBridgeReleaseAssets.ps1` should behave as follows:

* prefer a signed release APK when local signing is configured
* record Android artifact metadata in `dist/PowerBridge-vX.Y.Z.android-build.json`
* allow debug APK fallback for local validation only
* warn clearly when the selected Android artifact is a debug fallback
* avoid implying a debug fallback is an official release artifact

`scripts/Publish-PowerBridgeRelease.ps1` should remain stricter:

* require a signed Android release build by default
* block publishing when Android artifact metadata shows a debug fallback
* block publishing unsigned Android artifacts by default
* allow unsigned/debug publication only through an explicit override
* make any override obvious and intentional

## Windows Companion Signing

The current Windows Companion is PowerShell-based and may be unsigned during early public releases.

If the project later ships a packaged Windows executable or installer:

* Windows SmartScreen may warn if the publisher is unknown
* code signing should use a private certificate
* the private certificate must not be committed
* signing should happen in a controlled release environment
* installer signing should be documented before broad consumer release

The installer may target a local path under:

```text
C:\Tools
```

That install path should remain configurable if packaging requirements change.

## Windows Signing Material

Do not commit:

* code-signing certificates
* private keys
* `.pfx` files
* certificate passwords
* timestamping credentials
* private signing scripts containing secrets
* screenshots showing certificate paths or passwords

## Release Truth Rules

Do not claim a build is signed unless the signing step was actually performed and verified.

Do not claim a release is production-ready if it uses:

* debug APK
* unsigned Android release APK
* unsigned Windows installer without clear user warning
* unverified release artifacts
* missing checksums
* missing install notice
* missing release notes
* generated local setup files as release content

## Do Not Publish Signing Material

Do not publish or distribute signing material in:

* source control
* GitHub releases
* release assets
* issue attachments
* discussions
* screenshots
* diagnostics
* QR payloads
* generated setup JSON files
* documentation examples
* support logs

## Repo Hygiene Rules

Before committing or publishing, verify that none of these are staged:

* `android/signing.properties`
* `android/keystore.properties`
* `local.properties`
* keystores
* signing keys
* `.pfx` files
* `.pem` files
* `.key` files
* passwords
* tokens
* generated APKs
* generated AABs
* generated EXEs
* generated ZIPs
* generated diagnostics ZIPs
* generated QR images with real local values
* generated setup JSON payloads with real local values
* screenshots showing real local values

## Public Release Requirements

Official public Android release assets should use:

* locally signed release APK
* verified APK signature
* SHA-256 checksum
* current `INSTALL-NOTICE.txt`
* current `BUILD.md`
* current release notes
* public-value sweep
* clean Git status before publishing

Official Windows Companion release assets should use:

* normal installer executable when packaging is ready
* clear unsigned-package warning if not signed
* SHA-256 checksum
* current install notice
* current release notes

## v1.0.0 Signing Gate

Do not publish PowerBridge `v1.0.0` unless signing and packaging are release-appropriate.

For `v1.0.0`, the expected release state is:

* Android APK is a signed release build
* APK signature is verified
* APK checksum is generated
* Windows Companion installer exists
* Windows Companion signing state is clearly documented
* release assets are named consistently
* no signing material is included
* no generated diagnostics, QR payloads, setup JSON, logs, screenshots, or local values are included
* all selected final methods are working, tested, documented, and consumer-ready

## Added Recommendations

Before broad public release:

* create and back up the Android release keystore before final release candidates
* store the keystore outside the repository and outside casual synced folders
* use a password manager for signing passwords
* document who controls signing material
* verify the signed APK with `apksigner`
* generate SHA-256 hashes from the exact release files
* test installing the signed APK on a clean device
* avoid changing signing keys after public release unless a migration plan exists
* prefer signed Windows installer packaging before broad consumer release
* keep unsigned Windows distribution clearly labeled if used during early releases
* run the public-value sweep before publishing
* verify GitHub Actions are green before publishing
* keep `v1.0.0` reserved for a fully consumer-ready release
