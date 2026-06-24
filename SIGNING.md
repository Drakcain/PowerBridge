# Signing Notes

## Android

Debug APKs are not production-signed.

Release builds must be signed with a private keystore that is never committed to the repository.

Rules:

* do not commit `.jks`, `.keystore`, `.pfx`, or other signing files
* do not commit passwords or environment files containing signing secrets
* keep release signing material outside this repository

## Windows Companion

The current Windows Companion is PowerShell-based and may be unsigned in early public releases.

If the project later ships a packaged Windows executable or installer:

* Windows SmartScreen may warn if the publisher is unknown
* code signing should use a private certificate that is not committed
* signing should happen in a controlled release environment
* the installer may target a local path under `C:\Tools`, but that path must remain configurable by the installer if packaging requirements change

## Release Truth

Do not claim a build is signed unless the signing step was actually performed and verified.

Do not publish or distribute signing material in:

* source control
* release assets
* issue attachments
* screenshots
* documentation examples
