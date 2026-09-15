# InterpretaAI Runbook

## Curation Rules
- Re-prioritize on every read.
- Keep recurring, high-value notes only.
- Max 10 items per category.
- Each item includes date + "Do instead".

## Product Guardrails

1. **[2026-09-14] Remote AI never owns the child's hot path**
   Do instead: react locally, resolve unambiguous approved answers from the ScenePack, warm providers during narration, call LangChain4j only for open mediation on a HOT route, and keep RAG/LangGraph4j in asynchronous preparation or review flows.
2. **[2026-09-13] LEIA means Ler, Entender, Interpretar e Aprender**
   Do instead: use the exact definition as the product core and keep AI as a contextual mediator that encourages effort; never grade, diagnose, rank, or declare a child's emotion objectively correct.
3. **[2026-09-13] Child activities must fit one decision per viewport**
   Do instead: avoid required scrolling, keep one prominent next action, and reserve scrolling for educator views.
4. **[2026-09-13] Preserve the comic identity**
   Do instead: retain thick borders, hard shadows, speech balloons, large targets, and the yellow/red/green/blue palette.
5. **[2026-09-13] Offline behavior is part of the main experience**
   Do instead: keep prepared pedagogical speech and local TTS available, and visibly identify degraded operation without interrupting the activity.
6. **[2026-09-13] Conscious smartphone use must be visible in the journey**
   Do instead: use the device for a short guided cycle, then explicitly rest it so learning continues through pair or group interaction; describe socioemotional protection without clinical claims.
7. **[2026-09-13] Persist participation, never a verdict about the child**
   Do instead: store event, modality, duration and help; keep response routing transient and never reintroduce success, accuracy, grade or full transcript fields in pedagogical metrics.
8. **[2026-09-13] Make the puzzle consolidate comprehension, not replace it**
   Do instead: structure literacy cases as context → explicit information → clue → inference/explanation → language → practical application; add new cases only after the current contract is validated.
9. **[2026-09-13] Reveal support progressively and calibrate it pedagogically**
   Do instead: invite an oral/independent attempt before exposing answer-like choices, let the educator select 2x2 or 3x2 by the learning moment rather than age, and keep error feedback brief and non-repetitive.
10. **[2026-09-15] Separate the child avatar from institutional identity**
   Do instead: show only a pseudonymous avatar in child UI and metrics; keep any future name or enrollment mapping in a teacher-only identity store with RBAC and audit logs.

## Verification

1. **[2026-09-13] HACKTUDO compliance includes human evidence**
   Do instead: keep the official regulation PDF/hash, dependency credits, Git timeline and asset hashes reproducible; never call the project fully compliant until the team confirms eligibility, post-challenge conception, submissions, permissions and asset provenance.
2. **[2026-09-13] Audit all child screens at the target viewports**
   Do instead: verify 360x640, 412x915, and 800x1280 with no cut CTA, overlap, illegible text, or swipe-only action.
3. **[2026-09-13] Distinguish focus guarantees by device management**
   Do instead: test regular installs as user-confirmed screen pinning and managed tablets as Device Owner Lock Task; confirm `mLockTaskModeState=LOCKED` plus Home/Recent attempts.
4. **[2026-09-13] Preserve the focus test preconditions**
   Do instead: stop Lock Task through the educator screen before Compose tests, remove the instrumentation APK, and only then validate automatic focus.
5. **[2026-09-13] Visually validate the ten-page proposal**
   Do instead: render DOCX to page images/PDF and inspect every A4 page for cuts, blanks, broken tables, and exact page count.

## Release and Infrastructure

1. **[2026-09-14] ScenePack versions are immutable deploy artifacts**
   Do instead: publish a new bundled `scene-packs/vN.json`, activate it with `SCENE_PACK_VERSION`, verify `/api/v1/gateway/status`, and rollback by selecting the previous bundled version and restarting.
2. **[2026-09-14] Disable SDK retries explicitly on every conversational provider**
   Do instead: set `maxRetries(0)` for Ollama, NVIDIA and Gemini; use OkHttp 4.12 while the project remains on Kotlin 2.0, keep the single idempotent Android transport retry inside one global six-second deadline, remember legacy protocol detection per client, and verify failure latency with a deliberately unreachable endpoint.
3. **[2026-09-13] Never use `path` as a zsh loop variable**
   Do instead: use a task-specific name such as `target_file`; zsh ties `path` to `PATH` and overwriting it makes commands disappear inside that shell.
4. **[2026-09-13] Default to Qwen 2.5 1.5B, not the 3B variant**
   Do instead: keep the local Ollama default on the Apache-2.0 1.5B model and update credits before changing any model or voice weight.
5. **[2026-09-13] Never expose credentials to the Android client**
   Do instead: keep provider keys and credentials server-side and inject only the HTTPS voice API base URL at build time.
6. **[2026-09-13] Do not claim cloud or provider validation without evidence**
   Do instead: distinguish local implementation, public-tunnel demonstration, real provider smoke tests, and future deployment in every handoff.
7. **[2026-09-13] Avoid Gemini Developer API in child-facing flows under its current terms**
   Do instead: use a self-hosted model for the LEIA conversation unless a provider contract explicitly permits the intended under-18 audience and privacy requirements.
8. **[2026-09-13] Preserve port 8080 on this development Mac**
   Do instead: run the Spring MVP on 8088 because an existing `llama-server` uses 127.0.0.1:8080.
9. **[2026-09-13] Put delivery artifacts in predictable locations**
   Do instead: keep `dist/` as the only tracked delivery source, `output/screenshots/` as visual evidence, and the easy-send bundle on Desktop in `InterpretaAI-Entrega-11h45`.
10. **[2026-09-13] Treat Android speech privacy as device-dependent**
   Do instead: document `EXTRA_PREFER_OFFLINE` as a preference, validate the selected recognition service per device, and never promise local-only audio capture without that evidence.
