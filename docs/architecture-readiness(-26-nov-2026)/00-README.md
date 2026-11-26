# JWT-auth conversation archive

Six chronological prompt/answer documents, following the approved first export. File 01 and its assets are unchanged. Files 02–06 are reconstructed from the conversation: substantive tables, IDs, examples and diagrams are retained, but wording is condensed in places and diagrams are not pixel-identical. Original interactive citations are not reproduced. Maven versions and trees remain unverified historical proposals. The archive ends with the proposed architecture documentation package.

## Documents

- [01-stateless-jwt-and-in-memory-storage](01-stateless-jwt-and-in-memory-storage.md)
- [02-architecture-assumptions-and-constraints](02-architecture-assumptions-and-constraints.md)
- [03-rs256-jwk-jwks-and-certificates](03-rs256-jwk-jwks-and-certificates.md)
  - [03-a — Sign and verify with RSA and RS256](03-a-rsa-and-rs256.md)
  - [03-b — Compare PEM and JWK](03-b-pem-and-jwk.md)
  - [03-c — Rotate keys with JWKS and kid](03-c-jwks-and-key-rotation.md)
  - [03-d — Separate X.509 and TLS from JWT verification](03-d-x509-and-tls.md)
- [04-maven-dependencies-java17-plain-spring](04-maven-dependencies-java17-plain-spring.md)
- [05-clarifications-and-revision-requirements](05-clarifications-and-revision-requirements.md)
- [06-proposed-architecture-documentation-package](06-proposed-architecture-documentation-package.md)

## Assets

SVG primary, PNG fallback, Mermaid editable equivalent. Relative paths follow the approved first export (`assets/svg`, `assets/png`, `assets/mermaid`). Some reconstructed diagrams simplify the original layout.
