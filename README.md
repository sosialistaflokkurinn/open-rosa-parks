# Rósa Parks

**A public-interest project by Samtakamáttur svf., an Icelandic cooperative (samvinnufélag) under formation.** A parking-payment app for Reykjavík with no service mark-up — the user pays the city's statutory parking fee only; the app does not add a cent on top.

This open repository is a **deliberately limited mirror** of the private working repo. The mirror exists so that the kernel of the project is verifiable and so that the city can pick the code up and run it themselves; it is not intended as an active collaboration venue until the back-end connection with Bílastæðasjóður Reykjavíkur is granted.

## Status — August 2026

The project has been operated by **Samtakamáttur svf.**, an Icelandic cooperative (samvinnufélag) under formation, since July 2026.

A formal application for direct integration with the Bílastæðasjóður back-end was submitted on 24 April 2026 and the process with the city is ongoing. Until the city grants access, every payment made through the app counts as a **simulation only** — the city receives no notification that a vehicle has been parked, and the user remains liable to be ticketed. A warning screen shown on first launch must be explicitly acknowledged before the zone map is reachable (`BackendStatusScreen`, wired as the start destination in `NavGraph.kt`).

The Android app has been on the Google Play *Open Testing* track (Iceland) since 29 April 2026. The iOS app has been in TestFlight beta since July 2026.

## What's here

| Directory | Stack |
|-----------|-------|
| `app/`    | Android client — Kotlin + Jetpack Compose, min SDK 26 |
| `ios/`    | iOS client — Swift + SwiftUI, iOS 16+, XcodeGen |
| `worker/` | Back-end — TypeScript on Cloudflare Workers |

## What's **not** here

The mirror deliberately omits:

- Internal correspondence, application paperwork to Reykjavík city, and internal strategy notes
- CI/CD pipelines, deployment scripts, and signing-fingerprint inventory
- Tester email lists, developer credentials, and anything else covered by data-protection law
- Full commit history — the mirror is published as a single squashed snapshot per release cycle

Active development, code review, and decision-making happen in the private working repo until the back-end connection is in place. Once the city wires it up, the mirror will be re-cut on a proper synchronisation pipeline and contributions will be opened up.

## Releases

See the [Releases](https://github.com/samtakamattur/open-rosa-parks/releases) tab — release cycles are tagged there as they are published.

Upload-keystore fingerprint (SHA-256):
```
72:17:9D:AF:31:05:A0:0B:F1:A3:E6:E0:C9:BB:2B:45:BE:BD:87:07:55:41:E8:CC:15:9F:DD:C8:9A:4C:D9:50
```

## Why this exists

Reykjavík city already pays the contractor that runs the underlying parking-payment infrastructure — the part that actually matters. Everything else — the privately operated apps and the surcharges they levy on top of the city's fees — is a policy choice, not a technical necessity.

Samtakamáttur svf. publishes Rósa Parks to make that point in code: a fee-free parking app is technically trivial. The city is welcome to take this code over and operate it themselves — it ships under the [MIT licence](./LICENSE) precisely to make that path easy.

## Important disclaimer

This project is independent. It is **not** operated by, in partnership with, or with permission from Reykjavík city, Bílastæðasjóður Reykjavíkur, or any other government entity. Zone geography and tariff data are pulled from the city's own public sources:

- Borgarvefsjá: <https://borgarvefsja.reykjavik.is/borgarvefsja/>
- Reykjavíkurborg, parking: <https://reykjavik.is/bilastaedi>
- Bílastæðasjóður Reykjavíkur: <https://reykjavik.is/bilastaedasjodur>

## Privacy

See [`PRIVACY.md`](./PRIVACY.md) for a code-verified summary of what the app and back-end actually collect. The full policy is at <https://samtakamatt.is/personuvernd/rosaparks>.

## Contact

- `gudrodur@samtakamatt.is` — project + engineering
- Samtakamáttur svf. — Icelandic cooperative (samvinnufélag) under formation

## Licence

MIT — see [`LICENSE`](./LICENSE).
