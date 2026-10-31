# P5 — Cross-mode comparison and complete learning support

**Objective:** produce a trustworthy side-by-side reference for Spring MVC, HTTP Basic, and RS256 JWT.

## P5.S1 — Comparative runtime tests and diagrams
Run equivalent real HTTP requests in `none`, `basic`, `jwt` modes with isolated contexts. Capture sanitized, correlated trace events and assert key order invariants. Create three Mermaid sequence diagrams and a table of servlet filters, security filters, authentication providers, credential types, SecurityContext population, authorization outcomes, and response paths. Show successful and denied requests in secured modes; none has no security denial, so use the already-tested 404/500 paths when contrasting ordinary application errors. Compare the same requests and identities without suggesting that an invalid credential is automatically ignored because an endpoint is public.
**Acceptance:** full matrix passes and trace evidence is reproducible.

## P5.S2 — Learning package, review and final verification
Audit every application class/method/meaningful code line against `docs/learning/` walkthroughs; explain framework-owned behavior, configuration, positive and negative tests. Add `docs/learning/COMPLETE-GUIDE.md` linking all walkthroughs and cross-referencing test cases, plus interview-style questions and exercises. Verify no private keys/secrets logged or committed. Run `mvn verify` on the intended JDK 21 runtime, report the actual runtime and resolved framework versions plus exact executed tests, and update README/ROADMAP, `docs/FLOWS-COMPARED.md`, and `docs/TEST-MATRIX.md`.
**Acceptance:** if JDK 21 is unavailable, record that precise remaining verification gap; a newer-JDK release-21 build alone does not satisfy the runtime check. An engineer can reconstruct each request flow from docs and tests; all phase exit gates met.

## Exit criteria
Project remains compact and runnable, but teaching material is comprehensive, code-accurate and evidence-backed.
