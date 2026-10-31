# Understanding the Plain MVC, Basic, and JWT Flows

This guide compares plain MVC, HTTP Basic authentication, and RS256 JWT authentication in the same application at different learning stages. Plain MVC explains HTTP processing; Basic adds password-based authentication; JWT separates initial credential authentication from subsequent bearer-token verification. P1.S1 and P1.S2 are implemented and verified. Basic and JWT remain planned, so their descriptions explain intended architecture rather than observed runtime results.

## The shared foundation

Every mode starts with an HTTP client connecting to embedded Tomcat. Tomcat parses the request and passes servlet request and response objects through the matching servlet filters. Eventually, an allowed application request reaches Spring MVC’s `DispatcherServlet`.

The dispatcher asks handler mappings which handler matches the path and HTTP method. `RequestMappingHandlerMapping` connects annotations such as `@GetMapping` to controller methods. `RequestMappingHandlerAdapter` invokes the selected method. For a `@RestController`, response-body processing selects an HTTP message converter; Jackson converts the returned Java map into JSON. The response travels back through the surrounding infrastructure to the client.

Security changes what happens before controller execution. It does not replace Tomcat, MVC routing, controller invocation, or JSON serialization. A rejected request can finish without reaching the controller at all.

## Plain MVC: processing without security

The implemented baseline exposes GET `/api/public`, `/api/user`, and `/api/admin`. Each returns HTTP 200 and a deterministic JSON message without credentials. The names “user” and “admin” currently impose no restrictions.

Plain mode still has a servlet filter chain. Spring Boot can register infrastructure filters for concerns such as character encoding and request context. What is absent is Spring Security: no security dependency, `FilterChainProxy`, authentication provider, or authorization policy exists in P1.S1.

This distinction matters because `permitAll()` would allow access while retaining security processing. It would not reproduce an application without Spring Security. Current tests prove the three HTTP responses, and dependency inspection confirms security’s absence. P1.S2 now verifies correlated ordering: filter entry, interceptor preHandle, controller, interceptor postHandle, interceptor afterCompletion, filter exit. Server-generated response IDs identify bounded, immutable event snapshots. Concurrent-request tests check isolation. Tracing covers synchronous REQUEST dispatches; error-path tests remain P1.S3 work. See the [P1.S2 walkthrough](learning/P1.S2-WALKTHROUGH.md) for actual events and code references.

## HTTP Basic: authenticate credentials on requests

The planned Basic mode inserts Spring Security before MVC. A servlet `DelegatingFilterProxy` delegates to `FilterChainProxy`, which selects a matching `SecurityFilterChain`. When Basic credentials are supplied, `BasicAuthenticationFilter` extracts them and submits an authentication request to an `AuthenticationManager`.

A `DaoAuthenticationProvider` obtains the user through `InMemoryUserDetailsManager` and checks the supplied password using a `PasswordEncoder`. Successful authentication makes the identity and authorities available through the `SecurityContext`. Authorization then decides whether that identity may access the requested endpoint.

The intended policy permits public access, requires authentication for `/api/user`, and requires ADMIN for `/api/admin`. Missing or invalid credentials on protected resources produce 401, with an appropriate Basic challenge. An authenticated USER requesting the admin endpoint receives 403. Authentication establishes identity; authorization evaluates permission.

Basic encodes credentials rather than encrypting them, so transport protection matters. With the planned stateless configuration, requests authenticate independently instead of relying on a login session. Not every request executes a successful password check: absent headers, invalid credentials, and public access follow different branches.

## JWT: issue once, verify subsequent requests

JWT introduces two logically separate operations. First, the planned local POST `/auth/login` endpoint checks username and password through the authentication infrastructure. After success, `NimbusJwtEncoder` signs a token using an RSA private key. This demonstration endpoint is not an OAuth2 grant endpoint or authorization server.

Second, the client presents that token in an `Authorization: Bearer` header. Spring Security’s built-in `BearerTokenAuthenticationFilter` submits it for authentication. `JwtAuthenticationProvider` delegates decoding and verification to `NimbusJwtDecoder`.

The decoder must enforce RS256, verify the signature with the RSA public key, and validate required claims, including issuer, audience, and timestamps. These are distinct checks: a correct signature does not establish that a token targets this API or remains valid now.

After validation, `JwtAuthenticationConverter` maps trusted role claims into authorities. Spring Security creates the authenticated token representation and establishes the security context; Nimbus alone does not do that. Authorization then applies the endpoint rules before MVC proceeds.

Invalid tokens produce 401; authenticated callers lacking permission receive 403. JWT payloads are Base64URL encoded, not encrypted. Passwords and private keys must never appear in claims or logs.

## The practical difference

Plain MVC teaches request dispatch without identity checks. Basic repeatedly checks credentials; JWT checks a signed bearer credential after initial issuance. Both secured modes retain MVC and distinguish authentication from authorization. JWT adds cryptographic trust and claim validation, while moving password checking out of ordinary protected requests. Neither planned security flow is implemented by this document.
