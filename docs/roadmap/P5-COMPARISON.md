# P5 — Cross-mode comparison and complete learning support

**Objective:** produce a trustworthy side-by-side reference for Spring MVC, HTTP Basic, and RS256 JWT.

**Status:** P5 complete. Final clean Java21.0.8 verification passed 153 HTTP + 29 unit cases on 2026-11-01. Production behavior is unchanged.

## P5.S1 — Comparative runtime tests and diagrams

- [x] Verified 2026-11-01: `mvn verify` passed 153 HTTP integration + 29 unit cases, zero failures/errors/skips, 29.739 seconds (JDK 24/release 21).
- `NoneComparisonIT`, `BasicComparisonIT`, and `JwtComparisonIT` inherit one 13-row real HTTP contract in `AbstractSecurityComparison`: 39 new cases with isolated mode contexts.
- Tests compare anonymous/native identity access, USER/ADMIN authorization, wrong scheme, invalid credentials on public, login presence/failure, and authenticated missing routes. They assert status/challenge/body/cookies, infrastructure isolation and correlated callback invariants.
- Three Mermaid sequence diagrams and the servlet/security/provider/credential/context/authorization/response table are in [P5.S1’s walkthrough](../learning/P5.S1-WALKTHROUGH.md). Existing none ErrorPathIT remains the 404/500 dispatch evidence; no secured-mode synthetic failing endpoint added.
- At the S1 checkpoint, COMPLETE-GUIDE.md, the full audit and Java21 runtime check were pending. S2 below now completes them; the S1 JDK24 result remains historical.

Run equivalent real HTTP requests in `none`, `basic`, `jwt` modes with isolated contexts. Capture sanitized, correlated trace events and assert key order invariants. Create three Mermaid sequence diagrams and a table of servlet filters, security filters, authentication providers, credential types, SecurityContext population, authorization outcomes, and response paths. Show successful and denied requests in secured modes; none has no security denial, so use the already-tested 404/500 paths when contrasting ordinary application errors. Compare the same requests and identities without suggesting that an invalid credential is automatically ignored because an endpoint is public.
**Acceptance:** full matrix passes and trace evidence is reproducible.

## P5.S2 — Learning package, review and final verification

- [x] Final `mvn clean verify` passed on installed JetBrains Java21.0.8, 2026-11-01: 153 HTTP integration + 29 unit cases, zero failures/errors/skips, 29.403 seconds. Compiler and test JVM both use Java21; earlier JDK24-only gap is closed.
- [COMPLETE-GUIDE](../learning/COMPLETE-GUIDE.md) links all fourteen walkthroughs, audits all eighteen production Java files/methods/meaningful blocks, explains framework responsibilities and supplies questions/exercises.
- [P5.S2 walkthrough](../learning/P5.S2-WALKTHROUGH.md) records exact commands/runtime/dependencies, report-derived executed test inventory, actual final-run traces and secret/artifact audit results.
- Current summaries/source references refreshed; historical evidence retained and explicitly labeled. No production code/test behavior or additional feature added.
- Final log/current tracked files/13 reachable Git revisions contain no complete-token/private-PEM matches; no tracked key/environment paths or packaged PEM/test fixtures. Ignored local-key rule verified.

Audit every application class/method/meaningful code line against `docs/learning/` walkthroughs; explain framework-owned behavior, configuration, positive and negative tests. Add `docs/learning/COMPLETE-GUIDE.md` linking all walkthroughs and cross-referencing test cases, plus interview-style questions and exercises. Verify no private keys/secrets logged or committed. Run `mvn verify` on the intended JDK 21 runtime, report the actual runtime and resolved framework versions plus exact executed tests, and update README/ROADMAP, `docs/FLOWS-COMPARED.md`, and `docs/TEST-MATRIX.md`.
**Acceptance:** if JDK 21 is unavailable, record that precise remaining verification gap; a newer-JDK release-21 build alone does not satisfy the runtime check. An engineer can reconstruct each request flow from docs and tests; all phase exit gates met.

## Exit criteria
Project remains compact and runnable, but teaching material is comprehensive, code-accurate and evidence-backed.
