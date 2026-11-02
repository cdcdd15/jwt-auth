# Mandatory learning support

The project teaches Spring internals through executed HTTP evidence. Working code without its learning walkthrough is incomplete. [ROADMAP](../ROADMAP.md) is the active nine-step specification for Java 17 and unconditional JWT.

## Required artifacts for every step

Create `docs/learning/<step>-WALKTHROUGH.md` immediately, including documentation-only steps. Explain:

1. Objectives, prerequisites, scope and changes from the preceding checkpoint.
2. Every changed class, method, annotation, constructor dependency, private helper, configuration lambda and meaningful line/block. For documentation-only steps, explain document ownership, the preserved source concepts and deletion/replacement mapping. Do not invent code changes.
3. Concrete source links and accurate references checked against the current source. Distinguish framework-managed behavior from application code; avoid large unexplained code dumps.
4. Client → Tomcat → servlet filters → DelegatingFilterProxy/FilterChainProxy → bearer authentication → authorization → DispatcherServlet → handler mapping/adapter → controller → message conversion → unwind. Explain early rejection and ordinary ERROR dispatch separately.
5. Credential ProviderManager/DaoAuthenticationProvider/UserDetailsService/BCrypt versus bearer JwtAuthenticationProvider/Nimbus/strict decoder/converter. Explain who establishes and clears SecurityContext.
6. Mermaid sequence diagrams with concrete classes/methods and failure branches. Avoid raw semicolons in sequence-message labels because supported IDE renderers may parse them as statement separators.
7. A test-to-code table showing requests, expected status/body/headers, reached classes, security decisions and asserted evidence. Do not add a repeated standalone integration-test tutorial.
8. Actual sanitized correlated output from passing tests, with order and evidence limits. Never fabricate runtime traces or infer every internal call from application markers.
9. At least three review questions and two safe exercises with predicted results. Compare credential issuance with bearer verification or earlier migration checkpoints without presenting retired modes as supported configurations.
10. Actual JDK/Maven command, suite names/counts, failures/errors/skips and precise blockers. Update progress only after execution.

## Technical accuracy

- Resource Server JWT validation does not require an OAuth2 grant or authorization server.
- Nimbus verifies/decodes; Spring Security creates the authenticated representation and request SecurityContext after validation/conversion.
- Signature verification, algorithm restriction, normalized semantic checks, original JSON type checks, authentication and endpoint authorization are separate decisions.
- `permitAll()` preserves security processing. A supplied invalid bearer can fail before public authorization.
- Base64URL is readable encoding, not encryption. Do not include secrets in claims, traces or example tokens.
- Configured filter membership/order, observed entry, application callback snapshots and test-only ERROR probes are different evidence types. Label each.
- A finally/exit event is not proof of authorization success. No session cookie is bounded evidence, supplemented by context policy and fresh-request tests.
- Production traces cover synchronous REQUEST dispatches. Test-only ERROR probes explain redispatch without appending to completed snapshots. Async/distributed tracing is excluded.

## Maintenance and navigation

[PROJECT-STRUCTURE](PROJECT-STRUCTURE.md) owns inventory; [TEST-MATRIX](TEST-MATRIX.md) owns current executed coverage; [JWT-CLAIM-CONTRACT](JWT-CLAIM-CONTRACT.md) owns token acceptance; [learning index](learning/README.md) owns step navigation. [JWT foundations](learning/JWT-FOUNDATIONS.md) preserves prerequisite MVC/security/source explanations extracted during documentation replacement. The complete source-guide audit is J3.S2; final repository audit is J3.S3.

Retain earlier J walkthroughs as clearly labeled checkpoint records, including their actual historical counts and retired source names where needed to explain the migration. Current summary documents must give current contracts/counts only. Refresh moved source references or mark them superseded and link the current explanation. Remove links to deleted documents, check local links and parse Mermaid diagrams before claiming documentation verification. Preserve the original course through `master` and Git history rather than duplicating it here.
