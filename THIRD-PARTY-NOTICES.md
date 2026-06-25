# Third-Party Notices

PowerBridge uses third-party platforms, libraries, frameworks, runtimes, and build tools. Their licenses, copyrights, trademarks, and terms remain with their respective owners.

This file is an attribution and release-hygiene notice. It is not a replacement for each dependency’s own license text, package metadata, source repository, or upstream documentation.

## Scope

The MIT License in this repository applies only to original PowerBridge code, project-owned documentation, and project-owned assets.

It does not relicense:

* Android platform components
* Android SDK tooling
* Gradle or build tooling
* Kotlin tooling
* AndroidX libraries
* Material Components
* OkHttp
* ZXing
* QRCoder
* PowerShell
* .NET / WPF components
* operating-system components
* bundled third-party binaries
* upstream documentation copied by reference
* third-party trademarks, icons, names, or logos

Third-party dependencies keep their own licenses and terms.

## Android Platform and Tooling

PowerBridge Android is built with third-party and platform tooling including:

* Gradle
* Android Gradle Plugin
* Kotlin
* Android SDK
* JDK / Java tooling

These tools are used for building, packaging, testing, and validating the Android app.

## Android Libraries

The Android app currently depends on libraries including:

* AndroidX Core KTX
* AndroidX AppCompat
* Material Components for Android
* AndroidX ConstraintLayout
* AndroidX Security Crypto
* OkHttp
* Kotlin Coroutines for Android
* ZXing Android Embedded

Dependency versions should be reviewed from the Gradle build files before publishing a release.

## Windows Companion

The Windows Companion uses Windows and PowerShell-based tooling including:

* PowerShell 7+
* WPF / .NET on Windows for the GUI path
* QR generation support under `windows-companion/lib/`

The QR generation library or bundled binary under `windows-companion/lib/` must retain its upstream license and attribution requirements.

## Bundled Third-Party Files

If a third-party binary, library, or generated dependency is bundled with PowerBridge release assets, verify that:

* redistribution is allowed
* required license text is included
* required notices are included
* required copyright statements are preserved
* version/source information is documented where practical
* the bundled file is not mistaken for original PowerBridge code

Do not remove upstream license headers from third-party files.

## Release Hygiene

Before publishing a public release:

* review Android Gradle dependencies
* review Windows Companion bundled libraries
* verify third-party license requirements
* verify `THIRD-PARTY-NOTICES.md` is current
* verify required upstream license files are included when redistribution requires them
* verify release assets do not include unnecessary dependency caches or build outputs
* verify generated artifacts are not published unless they are intentional release assets

## What Not To Include

Do not include the following as third-party release content unless there is a specific, reviewed reason:

* Gradle caches
* Android build folders
* dependency cache folders
* local SDK files
* generated intermediate build outputs
* local package-manager caches
* diagnostics ZIPs
* generated QR payloads
* generated setup JSON files
* logs
* local screenshots with real values
* signing keys
* keystores
* signing properties
* `local.properties`

## Trademarks

Third-party names, platform names, product names, and logos belong to their respective owners.

References to Android, Windows, PowerShell, Kotlin, Gradle, AndroidX, Material Components, OkHttp, ZXing, QRCoder, or other third-party technologies are for identification and compatibility documentation only.

PowerBridge is not endorsed by those third parties unless explicitly stated by the relevant owner.

## Licensing Rule

The MIT License in this repository applies to original PowerBridge material only.

Third-party libraries, frameworks, runtimes, tools, and bundled binaries retain their own licenses and terms. Review upstream licenses before redistribution, packaging, or public release.

## Added Recommendations

For release preparation:

* generate or review a dependency list before each public release
* keep a copy or link to upstream license text for redistributed libraries
* verify bundled QR generation support has proper attribution
* avoid bundling dependency caches or build folders
* keep Android APK and Windows Companion installer release assets clean and minimal
* review dependency license compatibility before `v1.0.0`
* update this file whenever a new library, framework, runtime, or bundled binary is added
