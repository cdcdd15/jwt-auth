# Mandatory Complete Learning Support — Codex Directive

**Primary purpose:** This repository is a runnable, evidence-backed course on Spring HTTP processing and Spring Security internals, not merely a working demo. Documentation is a required artifact of EVERY implementation step.

## Documentation Codex must create and maintain
For every step, add `docs/learning/<step-id>-WALKTHROUGH.md` (create folder when implementing). Explain:
1. **Learning objectives and prerequisites:** what changes from the previous step and why.
2. **Class-by-class explanation:** purpose, bean creation, interfaces, annotations, dependencies, injected collaborators, entry/exit points.
3. **Line-by-line / block-by-block explanation:** cover every meaningful line of application code, imports where relevant, annotations, configuration expressions, method parameters, return values, and exception handling. Use short, numbered code excerpts or file:line references; do not paste unexplained large files. Explain framework-managed code that is not visible in the repo separately.
4. **Exact request flow:** client → embedded Tomcat → servlet filter chain → Spring Security FilterChainProxy (when present) → DispatcherServlet → handler/controller, including response and exceptions. Mark which steps occur only in some modes.
5. **Security-specific internals:** Authentication vs Authorization; AuthenticationManager, Provider, UserDetailsService, PasswordEncoder, SecurityContext, EntryPoint/AccessDeniedHandler; JWT header/claims/signature; Nimbus decoder, validators, authentication converter, and RS256 public/private key separation.
6. **Sequence diagram (Mermaid)** with concrete method/class names and clear failure branches.
7. **Test-to-code traceability:** for every integration test, show request, expected status/body, classes reached, security decision, and assertions. Distinguish observable runtime evidence from assumptions about framework internals.
8. **Runtime trace examples:** actual sanitized log/event sequences from passing tests, with correlation ID and order. Do not fabricate trace output.
9. **Comparative notes:** what stayed the same and what changed versus previous security mode.
10. **Questions and exercises:** at least three review questions and two safe hands-on modifications, with expected outcomes.

## Accuracy and security rules
- Explain that Spring Security's OAuth2 Resource Server is usable for bearer JWT validation without implementing an OAuth2 Authorization Server or any OAuth2 grant flow.
- Do not claim Nimbus alone creates an authenticated SecurityContext; Spring Security does that after decoding and converting a validated token.
- Do not call `permitAll` an absent security chain.
- Separate signature verification, issuer/audience/timestamp validation, authentication creation, and authorization decisions.
- Do not treat Base64URL payloads as encrypted; do not store secrets in JWT claims.
- Verify filter order against the actual Spring Security runtime, not a generic diagram.
- Record exact file paths and code references **after** implementation; never invent line numbers.
- Never include passwords, signing private keys, or full tokens in traces.

## Cross-phase reference documents
Update `docs/FLOWS-COMPARED.md` and `docs/TEST-MATRIX.md` as phases complete. Each should distinguish planned design from implemented and tested behavior. Final P5 produces a unified learning guide and side-by-side flow diagrams.

**Acceptance rule:** A step without its detailed learning walkthrough is incomplete even if all code tests pass.
