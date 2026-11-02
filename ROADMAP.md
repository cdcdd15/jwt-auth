# JWT-only learning roadmap — Java 17

## Step status

| Step | Description | Status |
|---|---|---|
| J1.S1 | Migrate the build and source to Java 17 | Complete — verified 2026-11-02 |
| J1.S2 | Make JWT unconditional and retire other modes | Complete — verified 2026-11-02 |
| J1.S3 | Simplify the remaining JWT components | Planned |
| J2.S1 | Consolidate login, authentication and authorization tests | Planned |
| J2.S2 | Consolidate strict validation and key-failure evidence | Planned |
| J2.S3 | Consolidate diagnostics, lifecycle and isolation evidence | Planned |
| J3.S1 | Replace obsolete course documentation | Planned |
| J3.S2 | Assemble the complete JWT learning guide | Planned |
| J3.S3 | Final Java 17 verification and repository audit | Planned |

Update this table and the corresponding checkbox only after the step is verified. Branch creation remains a separate, user-owned manual prerequisite.

## Objective, prerequisite and status

Create a focused JWT learning application from the completed three-mode course. Keep comprehensive explanations of Spring internals while removing none/basic implementations, mode selection, comparison scaffolding and historical course documentation from the JWT-only branch.

**Manual prerequisite — user-owned:** create and switch to branch `jwt-only` from the completed `master`. Branch creation is not an implementation step. Preserve the original course and Git history on `master`; do not rewrite history.

**Status:** J1.S1–J1.S2 complete; J1.S3–J3.S3 planned. The migration has **three phases, each containing three numbered steps**: nine implementation steps in total. The application now builds and runs on Java17 with unconditional JWT security; none/basic modes and mode selection are removed. Branch `jwt-only` is selected.

## Fixed architecture and learning requirements

- **Java 17 only:** Maven execution, compilation, tests and application runtime must use JDK 17. Compile with release 17 and enforce Maven's Java version range `[17,18)`. A newer JDK targeting release 17 does not satisfy runtime acceptance. Audit for APIs introduced after Java 17, including `List.getFirst()`.
- Retain Spring Boot 3.x, Spring MVC, Spring Security Resource Server, Nimbus, Maven and embedded Tomcat. Keep dependency versions/package names stable unless an evidenced Java 17 compatibility or cleanup requirement needs a change.
- JWT is unconditional. Remove `app.security-mode`, none/basic configurations and unsecured fallback. Public authorization still traverses the security chain.
- Use built-in bearer authentication and RS256. Preserve local JSON POST `/auth/login`, in-memory users, BCrypt credential checking, signing/verification key separation, strict raw/normalized claims and role conversion. No custom bearer authentication filter or replacement cryptographic parser.
- Preserve public/user/admin endpoint behavior, stateless security-context handling, bounded sanitized request/security/MVC tracing and identity isolation.
- Missing, invalid, weak or mismatched required keys fail startup. Never log passwords, full bearer tokens or private keys. Keep local development keys ignored and test fixtures out of the executable jar.
- No OAuth2 authorization/grant flow, authorization server, external IdP, database, refresh/revocation system, Docker or frontend.
- Integration tests exercise real HTTP through embedded Tomcat with `@SpringBootTest(RANDOM_PORT)`, RestAssured and Maven Failsafe. Unit tests supplement them. Isolate contexts where test configurations differ.
- Learning documentation is mandatory at every step: current classes/methods/annotations/meaningful blocks, framework behavior, request lifecycle, Mermaid sequences, failure branches, actual sanitized traces, test-to-code map, review questions and exercises.

## Phase J1 — Establish the Java 17 JWT-only application

### J1.S1 — Migrate the build and source to Java 17

Verified 2026-11-02: `mvn clean verify` on Eclipse Temurin17.0.21 passed 153 HTTP + 29 unit cases, zero failures/errors/skips (29.550 seconds). JDK21 and JDK24 `mvn validate` attempts failed with the intended version message. Three test getFirst calls became get(0); application dependencies and security behavior remain unchanged. [Walkthrough](docs/learning/J1.S1-WALKTHROUGH.md).

- [x] Configure compiler release 17 and a Maven Java-version gate accepting only `[17,18)`.
- Replace post-Java-17 source/test APIs and language usage with Java 17 equivalents without changing behavior.
- Review resolved dependencies and test instrumentation for actual JDK 17 compatibility; change only what verification requires.
- Update active build instructions and record the selected JDK 17 vendor/version and exact command.

**Acceptance:** `mvn clean verify` succeeds with actual JDK 17 across the existing suite. Maven rejects an unsupported runtime with a clear version requirement. No release-21 setting or post-17 API remains in active code/build configuration. If JDK 17 is unavailable, identify the precise blocker; do not substitute another runtime or claim this step complete.

### J1.S2 — Make JWT unconditional and retire other modes

Verified 2026-11-02: Java17 `mvn verify` passed 94 HTTP + 23 unit cases, zero failures/errors/skips (20.310 s). Missing-key context startup fails without mode selection; real Tomcat reports one security chain/proxy registration. Retired mode/comparison suites are removed and MVC/error/tracing tests now use signed bearer credentials. [Walkthrough](docs/learning/J1.S2-WALKTHROUGH.md).

- [x] Remove `BasicSecurityConfig`, `SecurityModeConfig`, the selector property and JWT-only conditional annotations.
- Activate JWT security, local login, issuer and decoder unconditionally; review Boot exclusions and servlet registration for exactly one intended chain/proxy, with outer tracing preserved.
- Remove none/basic-only suites, selector tests and cross-mode comparison scaffolding in this step so obsolete contracts do not leave the build broken. Remove mode properties from retained JWT test bootstraps.
- Preserve useful MVC/error/tracing tests by adapting their immediate authentication/key prerequisites; later steps refine their JWT coverage.

**Acceptance:** the application requires configured keys and uses JWT security without a mode property. None/basic cannot be selected. Public requests still traverse security; Basic credentials cannot authenticate protected routes. The retained suite passes `mvn verify` on JDK 17. Record deleted files, test coverage retained/reassigned and changed wiring in the step walkthrough.

### J1.S3 — Simplify the remaining JWT components

- [ ] Retain and simplify the in-memory credential/provider configuration, dedicated login manager, BCrypt, Nimbus encoder/decoder, strict claim guard and role converter.
- Remove leftover mode-specific branches, imports, redundant wiring and abstractions; avoid unrelated renaming or feature expansion.
- Preserve the three API endpoints, JSON token response, fixed issuer/audience, 300-second lifetime, explicit 60-second skew and fail-closed key loading.
- Keep handler responses and tracing behavior intact while eliminating unnecessary configuration left by the original isolation mechanism.

**Acceptance:** login-issued tokens work on protected routes; wrong credentials and invalid bearer inputs give 401, insufficient roles give 403, and missing/invalid/mismatched keys fail startup. No duplicate identity/security infrastructure remains. Run `mvn verify` on JDK 17 and explain every changed meaningful code block.

## Phase J2 — Prove JWT behavior through real HTTP

### J2.S1 — Consolidate login, authentication and authorization tests

- [ ] Consolidate retained HTTP coverage around public, user, admin and local login endpoints rather than historical mode comparisons.
- Verify valid USER/ADMIN login, trusted claim contents, issued-token use, generic bad-credential 401 and malformed-body 400.
- Verify anonymous access, correct bearer use, Basic-only rejection, role conversion, USER/admin denial and ADMIN access.
- Preserve absent/empty roles as valid authentication without ADMIN permission, and prove no session-cookie authentication dependency.

**Acceptance:** real HTTP tests assert statuses, appropriate JSON/challenges/cache headers and cookies where relevant, with deterministic identities/time and no credential logging. Run `mvn verify` on JDK 17 and publish the current method/scenario inventory rather than retaining old course totals.

### J2.S2 — Consolidate strict validation and key-failure evidence

- [ ] Retain raw-token tests for tampering, unknown RSA key, wrong algorithm, malformed serialization, issuer/audience and required claims.
- Preserve original JSON type checks, including numeric subject and explicit null timestamps, and malformed-versus-missing role behavior.
- Verify exact expiration/not-before boundaries against the controlled clock and configured skew, alongside permitted optional-claim/audience representations.
- Retain focused key-format, key-strength, matching-pair and startup failure checks; keep private fixture files owner-only and cleaned up.

**Acceptance:** invalid authentication fails closed before application handlers; allowed representations still work. Tests exercise the actual Nimbus/strict-decoder pipeline, not only normalized maps. Run `mvn verify` on JDK 17, distinguish parsing/signature/claim validation from authorization, and document actual results and fixture limitations.

### J2.S3 — Consolidate diagnostics, lifecycle and isolation evidence

- [ ] Retain configured-chain and observed-filter-entry checks, distinguishing early bearer rejection, anonymous challenge, authenticated role denial and successful MVC handoff.
- Verify correlated traces, server-thread post-proxy cleanup, authenticated-then-anonymous requests and concurrent identity separation.
- Adapt useful ordinary 404/500 and servlet/MVC lifecycle tests to JWT security with explicit authentication prerequisites; keep throwing handlers and observation probes test-only.
- Verify captured output and sink records omit passwords, full tokens and private material; preserve bounded immutable snapshots and REQUEST/ERROR scope distinctions.

**Acceptance:** real HTTP evidence reconstructs successful and failed request paths without inventing per-filter events or confusing ordinary errors with security denial. Run `mvn verify` on JDK 17 and document exact trace-to-code mappings, observed order and evidence limits.

## Phase J3 — Deliver the JWT-only learning package and final audit

### J3.S1 — Replace obsolete course documentation

- [ ] Remove the former P1–P5 phase specifications, historical walkthroughs and cross-mode comparison documents from this branch after extracting explanations needed by the JWT-only learning path.
- Rewrite README, AGENTS, scope, project structure and test matrix for unconditional JWT and Java 17. Preserve this roadmap, its progress and evidence.
- Refresh learning-support requirements and the JWT claim contract; remove stale mode selectors, file links, commands and historical test-count claims from active documentation.
- Keep the original course recoverable on `master` and through Git history rather than maintaining a second historical package on `jwt-only`.

**Acceptance:** active documentation describes the actual JWT-only code and Java 17 commands. Deleted-file references are removed, retained source explanations remain accessible, and the nine migration walkthroughs have a coherent home. Run `mvn verify` on JDK 17 and check local links and Mermaid syntax, including label-semicolon compatibility.

### J3.S2 — Assemble the complete JWT learning guide

- [ ] Create a focused complete guide linking all nine migration walkthroughs and the retained application/test contracts.
- Audit every remaining production class, method, annotation and meaningful line/block against current documentation, including private helpers and configuration lambdas.
- Explain initial credential authentication versus later bearer verification, RSA signing/public verification, raw versus normalized validation, SecurityContext establishment and endpoint authorization.
- Include request sequences from Tomcat through servlet filters, security proxy, DispatcherServlet, mapping/adapter, controller and response handling, with success/failure Mermaid diagrams and actual sanitized traces.
- Add test-to-code cross-references, interview questions and safe exercises with expected outcomes.

**Acceptance:** a learner can reconstruct the current application from guide, code and executed tests. Authentication, authorization, library parsing and application validation remain distinct. No obsolete three-mode course narrative is required to understand the application. Run `mvn verify` on JDK 17 and validate documentation coverage/links.

### J3.S3 — Final Java 17 verification and repository audit

- [ ] Run `mvn clean verify` on actual JDK 17 and record Maven/JDK/vendor/framework versions, exact command, test names/counts and actual outcomes.
- Recheck unconditional startup, one security chain/proxy registration, fail-closed keys, Basic rejection and preserved endpoint/trace contracts using automated evidence.
- Scan working/tracked content, relevant Git history and verification logs for secret patterns; verify ignored private-key paths and absence of packaged keys/test fixtures.
- Audit remaining Java-version settings, removed-mode references, links, diagrams, source ranges and final documentation consistency.
- Mark roadmap completion only after verification; report changed/deleted files, observed behavior, learning artifacts and any precisely scoped unresolved limitation.

**Acceptance:** the JWT-only application and comprehensive teaching package are self-consistent, all retained tests pass on Java 17, unsupported runtimes are rejected, and no unintended security fallback or obsolete mode scaffolding remains. No further implementation step is implied.

## Execution rules and transition authority

Implement **one numbered step per request**, in order. Before editing code, read the working agreement, this roadmap and relevant current sources/documentation; state intended classes/tests. Each step delivers its learning explanation immediately rather than postponing it to J3. Refresh affected documentation or mark superseded references during the transition. Complete verification before checking off a step; do not claim unexecuted results.

This user-requested migration supersedes older Java 21 and three-mode-preservation requirements on branch `jwt-only`. Existing AGENTS/phase documents remain historical where they conflict with this roadmap until J3.S1 replaces them; unrelated security, testing and teaching requirements still apply. This file is the active migration specification. Do not continue the completed P1–P5 sequence or create a branch automatically: branch creation remains the user's manual prerequisite.
