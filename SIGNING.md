# Signing Notes

## Android Release Signing Foundation

PowerBridge Android release signing is local-only in this phase.

Current rules:

* keep the real keystore outside the repository
* keep real signing properties outside source control
* do not commit passwords
* do not commit private absolute paths
* do not assume GitHub Actions validation builds are official signed release builds

This phase supports four distinct Android artifact states:

* `debug APK`
  * output from `assembleDebug`
  * installable for development/testing
  * not an official production-signed release
* `unsigned release APK`
  * possible output from `assembleRelease` when signing properties are absent or incomplete
  * useful for CI validation only
  * not installable as an official signed public release artifact
* `locally signed release APK`
  * output from `assembleRelease` when local signing properties are present and valid
  * intended source for future official GitHub release APK assets
* `future CI-signed release APK`
  * not implemented in this phase
  * would require GitHub Actions secrets and deliberate workflow changes later

## Local File Layout

Use one of these ignored local files inside `android/`:

* `android/signing.properties`
* `android/keystore.properties`

Use the tracked example as the template:

* `android/signing.properties.example`

Required keys:

* `storeFile`
* `storePassword`
* `keyAlias`
* `keyPassword`

Example only:

```properties
storeFile=C:/Path/Outside/Repo/powerbridge-release.jks
storePassword=CHANGE_ME
keyAlias=powerbridge
keyPassword=CHANGE_ME
```

Do not commit the real file.

## Keystore Location

Store the real release keystore outside the repo, for example:

```text
C:\Secure\Android\powerbridge-release.jks
```

or another private location you control.

Do not store it under:

* `<repo-root>`
* `android/`
* `dist/`
* synced screenshots/docs folders

## Generate A Keystore

Example placeholder command:

```powershell
keytool -genkeypair -v -keystore "C:\Path\Outside\Repo\powerbridge-release.jks" -alias powerbridge -keyalg RSA -keysize 4096 -validity 10000
```

Replace the path with your own private keystore location.

## Gradle Behavior

Current Gradle behavior is intentionally safe:

* if `android/signing.properties` or `android/keystore.properties` exists and is complete, `assembleRelease` signs the Android release build locally
* if signing properties are absent or incomplete, `assembleRelease` still runs without local signing
* debug builds continue to work without any signing setup
* public GitHub Actions validation does not require private signing secrets

## Local Build Commands

Debug build:

```powershell
Set-Location "<repo-root>\android"
.\gradlew.bat assembleDebug --no-daemon
```

Release build:

```powershell
Set-Location "<repo-root>\android"
.\gradlew.bat clean assembleRelease --no-daemon
```

## Verify The Signed APK

Use `apksigner` after a local signed release build:

```powershell
& "<android-sdk>\build-tools\34.0.0\apksigner.bat" verify --verbose ".\app\build\outputs\apk\release\app-release.apk"
```

If the local build is unsigned, verification for official signing will not be meaningful. Fix signing properties first.

## SHA-256 Checksum

After producing the signed APK:

```powershell
Get-FileHash ".\app\build\outputs\apk\release\app-release.apk" -Algorithm SHA256
```

## Release Script Truth

`scripts/Build-PowerBridgeReleaseAssets.ps1` now behaves as follows:

* prefers a signed release APK when local signing is configured
* records Android artifact metadata in `dist/PowerBridge-vX.Y.Z.android-build.json`
* can fall back to the debug APK for local validation if signing is not configured
* warns when the Android artifact is a debug fallback rather than a signed release

`scripts/Publish-PowerBridgeRelease.ps1` is stricter:

* requires a signed Android release build by default
* blocks publishing when the Android artifact metadata shows a debug fallback
* only allows unsigned/debug publication if explicitly overridden

## Windows Companion

The current Windows Companion is PowerShell-based and may be unsigned in early public releases.

If the project later ships a packaged Windows executable or installer:

* Windows SmartScreen may warn if the publisher is unknown
* code signing should use a private certificate that is not committed
* signing should happen in a controlled release environment
* the installer may target a local path under `C:\Tools`, but that path must remain configurable by the installer if packaging requirements change

## Release Truth Rules

Do not claim a build is signed unless the signing step was actually performed and verified.

Do not publish or distribute signing material in:

* source control
* release assets
* issue attachments
* screenshots
* documentation examples
