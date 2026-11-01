# Understanding the Plain MVC, Basic, and JWT Flows

This guide compares plain MVC, HTTP Basic authentication, and RS256 JWT authentication in the same application at different learning stages. Plain MVC explains HTTP processing; Basic adds password-based authentication; JWT separates initial credential authentication from subsequent bearer-token verification. P1–P3 and P4.S1 are verified. JWT validation and issuance work.

## The shared foundation

Tomcat parses HTTP and passes servlet objects through matching filters. Allowed requests reach Spring MVC’s `DispatcherServlet`.

The dispatcher asks handler mappings which handler matches the path and HTTP method. `RequestMappingHandlerMapping` connects annotations such as `@GetMapping` to controller methods. `RequestMappingHandlerAdapter` invokes the selected method. For a `@RestController`, response-body processing selects an HTTP message converter; Jackson converts the returned Java map into JSON. The response travels back through the surrounding infrastructure to the client.

Security changes what happens before controller execution. It does not replace Tomcat, MVC routing, controller invocation, or JSON serialization. A rejected request can finish without reaching the controller at all.

## Plain MVC: processing without security

The implemented baseline exposes GET `/api/public`, `/api/user`, and `/api/admin`. Each returns HTTP 200 and a deterministic JSON message without credentials. The names “user” and “admin” currently impose no restrictions.

Plain mode still has a servlet filter chain. Spring Boot can register infrastructure filters for concerns such as character encoding and request context. P1 originally excluded security libraries. P2.S1 includes them but registers no security proxy or chain in `none`; HTTP tests verify that absence and ignored Authorization headers.

This distinction matters because `permitAll()` would allow access while retaining security processing. It would not reproduce an application without Spring Security. Current tests verify responses, bean absence, and servlet registrations. P1.S2 now verifies correlated ordering: filter entry, interceptor preHandle, controller, interceptor postHandle, interceptor afterCompletion, filter exit. Server-generated IDs identify bounded snapshots; concurrent tests check isolation. Tracing covers synchronous REQUEST dispatches. P1.S3 verifies 404/500 JSON, skipped REQUEST postHandle, and subsequent ERROR handling by Boot’s error controller. See [error-path evidence](learning/P1.S3-WALKTHROUGH.md).

## HTTP Basic: authenticate credentials on requests

P2.S2 enables Basic with BCrypt-backed in-memory identities and a shared Basic challenge entry point. Missing or invalid credentials produce 401; invalid supplied Basic also fails on public routes. The implemented authentication flow follows: A servlet `DelegatingFilterProxy` delegates to `FilterChainProxy`, which selects a matching `SecurityFilterChain`. When Basic credentials are supplied, `BasicAuthenticationFilter` extracts them and submits an authentication request to an `AuthenticationManager`.

A `DaoAuthenticationProvider` obtains the user through `InMemoryUserDetailsManager` and checks the supplied password using a `PasswordEncoder`. Successful authentication makes the identity and authorities available through the `SecurityContext`. Authorization then decides whether that identity may access the requested endpoint.

The intended policy permits public access, requires authentication for `/api/user`, and requires ADMIN for `/api/admin`. Missing or invalid credentials on protected resources produce 401, with an appropriate Basic challenge. USER receives 403 on admin without a new challenge; ADMIN receives 200. Both identities can access user because it requires authentication. Authentication establishes identity; authorization evaluates permission.

Basic encodes credentials rather than encrypting them, so transport protection matters. With the implemented stateless configuration, requests authenticate independently instead of relying on a login session. Not every request executes a successful password check: absent headers, invalid credentials, and public access follow different branches.

## JWT: issue once, verify subsequent requests

JWT introduces two logically separate operations. First, the implemented local POST `/auth/login` endpoint checks username and password through the authentication infrastructure. After success, `NimbusJwtEncoder` signs a token using an RSA private key. This demonstration endpoint is not an OAuth2 grant endpoint or authorization server.

Second, implemented with test-issued and login-issued tokens, the client presents a token in an `Authorization: Bearer` header. Spring Security’s built-in `BearerTokenAuthenticationFilter` submits it for authentication. `JwtAuthenticationProvider` delegates decoding and verification to `NimbusJwtDecoder`.

The decoder must enforce RS256, verify the signature with the RSA public key, and validate required claims, including issuer, audience, and timestamps. These are distinct checks: a correct signature does not establish that a token targets this API or remains valid now.

After validation, `JwtAuthenticationConverter` maps trusted role claims into authorities. Spring Security creates the authenticated token representation and establishes the security context; Nimbus alone does not do that. Authorization then applies the endpoint rules before MVC proceeds.

Invalid tokens produce 401; authenticated callers lacking permission receive 403. JWT payloads are Base64URL encoded, not encrypted. Passwords and private keys must never appear in claims or logs.

## The practical difference

Plain MVC teaches request dispatch without identity checks. Basic repeatedly checks credentials; JWT checks a signed bearer credential after initial issuance. Both secured modes retain MVC and distinguish authentication from authorization. JWT adds cryptographic trust and claim validation, while moving password checking out of ordinary protected requests. See [Basic evidence](learning/P2.S3-WALKTHROUGH.md) and [JWT validation evidence](learning/P3.S2-WALKTHROUGH.md) for implemented behavior and actual events.

[Local issuance evidence](learning/P3.S3-WALKTHROUGH.md); [role conversion, 401/403, and session independence](learning/P4.S1-WALKTHROUGH.md).
