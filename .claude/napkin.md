# InterpretaAI napkin

## Product invariants

- LEIA (Ler, Escrever, Interpretar e Aplicar) is the product core; AI mediates context and never grades or diagnoses a child.
- Child flows use one decision per viewport and must not require scrolling; educator views may scroll.
- Preserve the comic identity: thick borders, shadows, speech balloons, yellow/red/green/blue palette.
- Metrics are pedagogical signals (participation, modality, help, time), never child rankings.
- Offline fallback is a first-class path and must be visibly identified.

## Verification

- Audit child screens at 360×640, 412×915, and 800×1280 before release.
- On the managed emulator, stop Lock Task through the educator screen before Compose instrumentation tests.
- Remove the instrumentation test APK before validating automatic focus; its presence intentionally suppresses auto-lock.
- Validate focus by checking `mLockTaskModeState=LOCKED` and attempting Home and Recent.
- Render the proposal DOCX to PNG/PDF and inspect all pages; the deliverable must remain exactly 10 A4 pages.

## Release

- Build artifacts live in `dist/`; the easy-send bundle lives on Desktop in `InterpretaAI-Entrega-11h45`.
- Never claim Cloud Run/Gemini/Chirp smoke as complete without actual credentials and endpoint evidence.
- Keep Gemini keys and Google ADC on the server; configure the Android base URL with `-PvoiceApiUrl`.

