# Docs Map

This folder contains the public-facing project docs for the PowerBridge monorepo.

## Core Project Docs

* [PHASE-PLAN.md](PHASE-PLAN.md) - public roadmap and current phase truth
* [PRIVACY.md](PRIVACY.md) - privacy stance and user-controlled data handling
* [PUBLIC-SANITIZATION.md](PUBLIC-SANITIZATION.md) - public safety and publish hygiene rules
* [RELEASE-CHECKLIST.md](RELEASE-CHECKLIST.md) - release prep and publish validation checklist
* [QR-CONTRACT.md](QR-CONTRACT.md) - current Windows Companion to Android QR import contract

## Home Device Relay Docs

* [HOME-DEVICE-RELAY-ARCHITECTURE.md](HOME-DEVICE-RELAY-ARCHITECTURE.md) - Phase 12 architecture
* [HOME-DEVICE-RELAY-CONTRACTS.md](HOME-DEVICE-RELAY-CONTRACTS.md) - Phase 13 contract definitions
* [HOME-DEVICE-RELAY-PROTOTYPE-PLAN.md](HOME-DEVICE-RELAY-PROTOTYPE-PLAN.md) - Phase 13 prototype planning and Phase 14 readiness

## Current Roadmap Truth

Current completed relay prep phases:

* `Phase 12` - Home Device Relay architecture
* `Phase 13` - Home Device Relay prototype planning and contract validation
* `Phase 13.5` - Pre-prototype cleanup and Phase 14 readiness audit

Next runtime phase:

* `Phase 14` - Home Device Relay prototype implementation

## Archive Hygiene

Project ZIP files are workstation snapshots, not source-of-truth development roots.

Current audit rule:

* `PowerBridge.zip` can be treated as a snapshot of the current monorepo state when comparison is needed
* `Win Companions.zip` is stale or superseded and should not be pulled back into active work

Do not commit project ZIP archives, extracted generated output, or stale comparison folders into the public repository.
