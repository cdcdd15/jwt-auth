# Codex Working Agreement

1. Read `README.md`, `ROADMAP.md`, `docs/LEARNING-SUPPORT-REQUIREMENTS.md`, `docs/PROJECT-STRUCTURE.md` and the active `docs/roadmap/P*.md` before changing code.
2. Implement **one numbered step** at a time. Do not preemptively implement future phases or broaden scope.
3. Before coding, state the intended classes, test cases, and how the step will be verified. Keep changes minimal and inspect existing code first.
4. Preserve the `none`, `basic`, and `jwt` behavior contracts. Never treat `permitAll()` as proof that Spring Security is absent in `none` mode.
5. No OAuth2 authorization flow, authorization server, grant types, redirect login, external IdP, database, Docker, or frontend. For JWT use the built-in Spring Security Resource Server integration with Nimbus and RS256; no custom bearer authentication filter.
6. Tests must exercise real HTTP via embedded Tomcat, RANDOM_PORT, RestAssured. Use isolated Spring application contexts per mode. Unit tests supplement integration tests, not replace them.
7. Every step requires learning documentation with class/method/line-level commentary, request sequence, actual framework behavior, failure branches, tests, and a trace-to-code map. Follow `docs/LEARNING-SUPPORT-REQUIREMENTS.md`.
8. Run `mvn verify` (or explain precisely why it cannot run); never claim a test passed without executing it. Include test names and results in handoff.
9. Never log passwords, complete bearer tokens, or private keys. Use local development keys only, exclude private keys from version control, and never put a production secret in configuration.
10. Update `ROADMAP.md` checkboxes and the current phase document only after verification. Stop at the requested step and summarize changed files, observed behavior, tests, documentation, and next step.
