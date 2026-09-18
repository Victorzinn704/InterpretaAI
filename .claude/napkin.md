# InterpretaAI Runbook

## Curation Rules
- Re-prioritize on every read.
- Keep recurring, high-value notes only.
- Max 10 items per category.
- Each item includes date + "Do instead".

## Product Guardrails

1. **[2026-09-14] Remote AI never owns the child's hot path**
   Do instead: react locally, resolve unambiguous approved answers from the ScenePack, warm prepared speech before optional model probes during narration, call LangChain4j only for open mediation on a HOT route, and keep RAG/LangGraph4j in asynchronous preparation or review flows.
2. **[2026-09-16] Distinguish LÉIA from LEIA everywhere**
   Do instead: use **LÉIA** only for the teacher/persona with her dog and **LEIA** for the method
   Ler, Entender, Interpretar e Aprender; keep AI as a contextual mediator that encourages effort
   and never grades, diagnoses, ranks, or declares a child's emotion objectively correct.
3. **[2026-09-17] Make Android layouts adaptive and reveal complexity progressively**
   Do instead: keep one child decision per viewport without required scrolling; let teacher views scroll, increase their tablet margins, and hide editing, signals and technical settings until the professor requests them.
4. **[2026-09-17] Keep one visual life across child and teacher experiences**
   Do instead: retain paper, ink, comic borders, hard shadows and the yellow/red/green/blue palette in the APK; use a calmer teacher notebook and the same visible story journey from review through classroom evidence.
5. **[2026-09-13] Offline behavior is part of the main experience**
   Do instead: keep prepared pedagogical speech and local TTS available, and visibly identify degraded operation without interrupting the activity.
6. **[2026-09-13] Conscious smartphone use must be visible in the journey**
   Do instead: use the device for a short guided cycle, then explicitly rest it so learning continues through pair or group interaction; describe socioemotional protection without clinical claims.
7. **[2026-09-13] Persist participation, never a verdict about the child**
   Do instead: store event, modality, duration and help; keep response routing transient and never reintroduce success, accuracy, grade or full transcript fields in pedagogical metrics.
8. **[2026-09-16] Make activities consolidate the story, not replace it**
   Do instead: structure literacy cases as context → explicit information → clue →
   inference/explanation → language → practical application; deliver approved content as an
   immutable `LearningStoryPack`, but admit authoring output only through `validateDraft` without
   approval claims, then freeze the deliverable snapshot after adult review of hash and images.
   Preserve its version from assignment through events and add a new mechanic only after app
   compatibility and contract validation.
9. **[2026-09-13] Reveal support progressively and calibrate it pedagogically**
   Do instead: invite an oral/independent attempt before exposing answer-like choices, let the educator select 2x2 or 3x2 by the learning moment rather than age, and keep error feedback brief and non-repetitive.
   Keep year filters organizational, retain an explicit all-years option for recomposition, and never turn grade into an automatic diagnosis or locked learning track.
10. **[2026-09-18] Keep child, teacher and device identities separate**
   Do instead: keep the teacher in the OIDC Studio, remove teacher navigation from the child Home,
   open adult device setup only through a protected deep link, and bind a school tablet to a learner
   only for an active classroom session. Keep names in teacher/setup views, send only a pseudonym and
   avatar into the child journey, cap rosters at 40, and release every tablet when the session closes.

## Verification

1. **[2026-09-13] HACKTUDO compliance includes human evidence**
   Do instead: keep the official regulation PDF/hash, dependency credits, Git timeline and asset hashes reproducible; never call the project fully compliant until the team confirms eligibility, post-challenge conception, submissions, permissions and asset provenance.
2. **[2026-09-13] Audit all child screens at the target viewports**
   Do instead: verify 360x640, 412x915, and 800x1280 with no cut CTA, overlap, illegible text, or swipe-only action.
3. **[2026-09-13] Distinguish focus guarantees by device management**
   Do instead: test regular installs as user-confirmed screen pinning and managed tablets as Device Owner Lock Task; confirm `mLockTaskModeState=LOCKED` plus Home/Recent attempts.
4. **[2026-09-13] Preserve the focus test preconditions**
   Do instead: keep Lock Task entry explicit in `MainActivity.onResume` (not automatic through the
   manifest), stop focus through the educator screen before Compose tests, remove the instrumentation
   APK, and only then validate automatic focus separately.
5. **[2026-09-13] Visually validate the ten-page proposal**
   Do instead: render DOCX to page images/PDF and inspect every A4 page for cuts, blanks, broken tables, and exact page count.
6. **[2026-09-16] Distinguish the identified SME purchase from the still-unknown GET fleet**
   Do instead: cite Comprasnet `prgCod=1057649` plus contracts 146/147 as proof that SME bought 13 Samsung Galaxy Tab A8 4G `SM-X205N` units at R$1,754.56; do not claim those units represent or were assigned to the GET fleet without inventory/MDM/device evidence, and keep finger input as the baseline because the separate 12.4-inch/S Pen specification belongs to another project.
7. **[2026-09-17] Clean authoring test data in dependency order**
   Do instead: delete `authoring_plan_queue` before `authoring_job_queue` and `authoring_job`; the preparation handoff creates a plan-queue row with a foreign key to the job.
8. **[2026-09-17] Refresh built Studio resources before visual audit**
   Do instead: run `./gradlew :server:processResources` before `tools/audit-studio-review.py`; that audit serves `server/build/resources/main/static/studio`, so source-only JavaScript edits otherwise leave screenshots and assertions testing stale code.
9. **[2026-09-17] Keep adult v2 fail-closed when OIDC is unavailable**
   Do instead: give `/api/v2/**` an explicit deny-all chain when OIDC is disabled, keep
   `/api/v2/devices/**` on its higher-priority device credential chain, and test that neither can
   fall through to the legacy v1 `permitAll` configuration.

## Release and Infrastructure

1. **[2026-09-14] ScenePack versions are immutable deploy artifacts**
   Do instead: publish a new bundled `scene-packs/vN.json`, activate it with `SCENE_PACK_VERSION`, verify `/api/v1/gateway/status`, and rollback by selecting the previous bundled version and restarting.
2. **[2026-09-16] Measure response phases; do not equate connection speed with child-perceived latency**
   Do instead: compare Gemini 3.8 Flash and Mistral with client p50/p95 plus envelope-v1 `serverElapsedMs` for ACK, validated text and complete audio; after `FINAL_TEXT`, allow remote audio only 650 ms before closing the stream and using local TTS, keep `maxRetries(0)` in every provider, one idempotent Android retry inside six seconds, and never stream unvalidated model tokens to the child. Renew the synthetic `HOT` probe before its 150-second TTL (90-second schedule); an on-demand probe may skip an already-hot route.
3. **[2026-09-13] Never use `path` as a zsh loop variable**
   Do instead: use a task-specific name such as `target_file`; zsh ties `path` to `PATH` and overwriting it makes commands disappear inside that shell.
4. **[2026-09-13] Default to Qwen 2.5 1.5B, not the 3B variant**
   Do instead: keep the local Ollama default on the Apache-2.0 1.5B model and update credits before changing any model or voice weight.
5. **[2026-09-13] Keep credentials and authorization decisions server-side**
   Do instead: keep provider keys server-side, keep only the public HTTPS base URL in the APK (Oracle is the release default and can be overridden at build time), use the
   OIDC subject only to locate active database memberships, derive role/school/classroom access from
   the database, and give each Android device a revogable credential with minimum scope.
   Do instead for the teacher web Studio: keep OIDC tokens server-side behind an authenticated
   session/CSRF BFF; never reuse the child APK's PIN or place adult bearer tokens in JavaScript.
   If any diagnostic prints a secret, stop and rotate every exposed source and downstream consumer;
   never use `export` without an assignment inside a remote debug command.
6. **[2026-09-18] Verify the teacher flow by independent gates**
   Do instead: verify release revision, Flyway, health, v1 gateway, OIDC token, membership, browser
   callback and Studio BFF separately. The Oracle pilot uses Keycloak 26.7.4 and passed those gates;
   keep the physical tablet journey and human study explicitly pending.
7. **[2026-09-13] Avoid Gemini Developer API in child-facing flows under its current terms**
   Do instead: use a self-hosted model for the LEIA conversation unless a provider contract explicitly permits the intended under-18 audience and privacy requirements.
8. **[2026-09-18] Prove Oracle recovery before database promotion**
   Do instead: run `verify-db-restore-drill.sh`, require PostgreSQL to start and answer from an ephemeral volume, then create a pre-change differential backup. Use private port 5432 for InterpretaAI until PgBouncer explicitly lists and tests its databases.
9. **[2026-09-18] Put delivery artifacts in predictable locations**
   Do instead: keep `dist/` as the tracked delivery index, `output/screenshots/` as visual evidence, and copy the verified APK, Oracle bundle and SHA-256 files to Downloads for delivery.
10. **[2026-09-13] Treat Android speech privacy as device-dependent**
   Do instead: document `EXTRA_PREFER_OFFLINE` as a preference, validate the selected recognition service per device, and never promise local-only audio capture without that evidence.
