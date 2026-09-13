# InterpretaAI Runbook

## Curation Rules
- Re-prioritize on every read.
- Keep recurring, high-value notes only.
- Max 10 items per category.
- Each item includes date + "Do instead".

## Product Guardrails

1. **[2026-09-13] LEIA is the product core**
   Do instead: use AI only as a contextual mediator that encourages effort; never grade, diagnose, rank, or declare a child's emotion objectively correct.
2. **[2026-09-13] Child activities must fit one decision per viewport**
   Do instead: avoid required scrolling, keep one prominent next action, and reserve scrolling for educator views.
3. **[2026-09-13] Preserve the comic identity**
   Do instead: retain thick borders, hard shadows, speech balloons, large targets, and the yellow/red/green/blue palette.
4. **[2026-09-13] Offline behavior is part of the main experience**
   Do instead: keep prepared pedagogical speech and local TTS available, and visibly identify degraded operation without interrupting the activity.

## Verification

1. **[2026-09-13] Audit all child screens at the target viewports**
   Do instead: verify 360x640, 412x915, and 800x1280 with no cut CTA, overlap, illegible text, or swipe-only action.
2. **[2026-09-13] Distinguish focus guarantees by device management**
   Do instead: test regular installs as user-confirmed screen pinning and managed tablets as Device Owner Lock Task; confirm `mLockTaskModeState=LOCKED` plus Home/Recent attempts.
3. **[2026-09-13] Preserve the focus test preconditions**
   Do instead: stop Lock Task through the educator screen before Compose tests, remove the instrumentation APK, and only then validate automatic focus.
4. **[2026-09-13] Visually validate the ten-page proposal**
   Do instead: render DOCX to page images/PDF and inspect every A4 page for cuts, blanks, broken tables, and exact page count.

## Release and Infrastructure

1. **[2026-09-13] Never expose credentials to the Android client**
   Do instead: keep provider keys and credentials server-side and inject only the HTTPS voice API base URL at build time.
2. **[2026-09-13] Do not claim cloud or provider validation without evidence**
   Do instead: distinguish local implementation, public-tunnel demonstration, real provider smoke tests, and future deployment in every handoff.
3. **[2026-09-13] Avoid Gemini Developer API in child-facing flows under its current terms**
   Do instead: use a self-hosted model for the LEIA conversation unless a provider contract explicitly permits the intended under-18 audience and privacy requirements.
4. **[2026-09-13] Preserve port 8080 on this development Mac**
   Do instead: run the Spring MVP on 8088 because an existing `llama-server` uses 127.0.0.1:8080.
5. **[2026-09-13] Put delivery artifacts in predictable locations**
   Do instead: keep build outputs under `dist/` and the easy-send bundle on Desktop in `InterpretaAI-Entrega-11h45`.
