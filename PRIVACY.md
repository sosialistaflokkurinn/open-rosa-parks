# Privacy — Rósa Parks

This file is a summary of what the code in this repository actually collects — every claim below is verifiable in the source files named. The full privacy policy (controller, legal basis, data location, your rights, deletion requests) is the authoritative document:

> **<https://samtakamatt.is/personuvernd/rosaparks>**

## What the app sends to the back-end

When you start a parking session (`POST /api/parking/start` in `worker/src/index.ts`):

- Vehicle registration number (*bílnúmer*) — as you type it
- Zone identifier (P1–P4) — as you select it
- Start timestamp; an end timestamp is recorded when you stop the session

That is the entire session record (`parking_sessions`: id, plate, zone, start, end). Payments are simulated until the city grants back-end integration, so nothing is forwarded to any parking operator.

If you sign up as a beta tester on the signup page, your email address, chosen locale, signup source, and a short user-agent prefix are stored (`beta_signups` in the same file).

## What stays on the device

- Location — used only to position you on the zone map; never sent to our servers.
- Preferences (map-first setting, the first-launch acknowledgement).

## Crash reporting

Both apps include **Firebase Crashlytics**: if the app crashes, a crash report (stack trace, device model, OS version) goes to Google so the crash can be fixed (`RosaParksApplication.kt` / `CrashlyticsTree.kt` on Android, `FirebaseCrashlytics` on iOS). There are no ads and no analytics or measurement SDKs — Crashlytics is the only third-party SDK in the apps.

## No accounts

No user accounts, no sign-in, no access to call history, contacts, photos, or microphone.

---

*This summary is refreshed with each published snapshot. For anything it does not answer — including access and deletion requests — use the full policy linked above.*
