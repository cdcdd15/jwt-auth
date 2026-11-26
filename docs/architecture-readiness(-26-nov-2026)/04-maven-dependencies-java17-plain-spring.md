# 04 — Maven dependencies — Java 17 plain Spring

## Original prompt

> A review of mvn depdcy.. plain Spring vers. Brief depd. tree format. Compatble versions with J17. Create 2 lists. One for spring sc. JWT only, one including all .jars (web, test..).

## Answer

## Maven dependency baseline — jwt-resource-server
Java 17; plain Spring Framework and Spring Security without Boot; RS256 using Nimbus; static RSA and remote JWKS; servlet REST; no DB, JPA, Hibernate, Redis, issuer implementation or OAuth login flow.

## 1. Java 17 compatible versions
| Technology | Proposed version |
|---|---|
| Java | 17 LTS |
| Maven | 3.9.x |
| Spring Framework | 6.2.19 |
| Spring Security | 6.5.11 |
| Nimbus JOSE + JWT | Transitive, managed through JOSE dependency |
| Jakarta Servlet API | 6.0.x |
| Apache Tomcat | 10.1.x |
| Jackson | 2.21.x |
| JUnit Jupiter | 5.14.x |
| Mockito | 5.x |
| SLF4J | 2.0.x |
| Logback | 1.5.x |

Spring 6.x selected as conservative baseline.

## 2. List #1 — Spring Security + JWT only
[D] Direct; [T] Transitive.

```text
jwt-resource-server
├── [D] spring-security-config
│   └── [T] spring-security-core
│       └── [T] spring-security-crypto
├── [D] spring-security-web
│   └── [T] spring-security-core
├── [D] spring-security-oauth2-resource-server
│   ├── [T] spring-security-core
│   ├── [T] spring-security-web
│   └── [T] spring-security-oauth2-core
└── [D] spring-security-oauth2-jose
    ├── [T] spring-security-core
    ├── [T] spring-security-oauth2-core
    └── [T] nimbus-jose-jwt
```
| JAR | Relevant components / responsibility |
|---|---|
| spring-security-config | HttpSecurity, security DSL |
| spring-security-core | Authentication, AuthenticationManager, SecurityContextHolder |
| spring-security-crypto | General cryptographic utilities |
| spring-security-web | SecurityFilterChain, FilterChainProxy, AuthorizationFilter |
| spring-security-oauth2-resource-server | BearerTokenAuthenticationFilter, JwtAuthenticationProvider |
| spring-security-oauth2-core | Bearer token abstractions, errors and validators |
| spring-security-oauth2-jose | Jwt, JwtDecoder, NimbusJwtDecoder |
| nimbus-jose-jwt | RS256, JWT parsing, JWK, JWKS, verification |
![Architectural layering](assets/svg/IMG12.svg)

[PNG](assets/png/IMG12.png) · [Mermaid source](assets/mermaid/IMG12.mmd)

RS256 does not require Bouncy Castle.

## 3. List #2 — Complete application dependency tree

```text
jwt-resource-server
├── SPRING FRAMEWORK [6.2.19]
│   ├── [D] spring-webmvc
│   ├── [T] spring-web
│   ├── [T] spring-context
│   ├── [T] spring-beans
│   ├── [T] spring-core
│   ├── [T] spring-jcl
│   ├── [T] spring-aop
│   ├── [T] spring-expression
├── SPRING SECURITY [6.5.11]
│   ├── [D] spring-security-config
│   ├── [D] spring-security-web
│   ├── [T] spring-security-core
│   ├── [T] spring-security-crypto
│   ├── [D] spring-security-oauth2-resource-server
│   ├── [D] spring-security-oauth2-jose
│   ├── [T] spring-security-oauth2-core
├── JWT / CRYPTOGRAPHY
│   ├── [T] nimbus-jose-jwt
│   ├── [T] jcip-annotations
├── SERVLET API [provided]
│   ├── [D] jakarta.servlet-api
├── SERVLET CONTAINER [external runtime]
│   ├── [P] Apache Tomcat 10.1.x
├── JSON SERIALIZATION
│   ├── [D] jackson-databind
│   ├── [T] jackson-core
│   ├── [T] jackson-annotations
├── OBSERVABILITY
│   ├── [T] micrometer-observation
│   ├── [T] micrometer-commons
├── LOGGING
│   ├── [D] slf4j-api
│   ├── [D] logback-classic
│   ├── [T] logback-core
├── TEST [test scope]
│   ├── [D] spring-test
│   ├── [D] spring-security-test
│   ├── [D] junit-jupiter
│   ├── [T] junit-jupiter-api
│   ├── [T] junit-jupiter-engine
│   ├── [T] junit-jupiter-params
│   ├── [T] junit-platform-engine
│   ├── [T] junit-platform-commons
│   ├── [T] opentest4j
│   ├── [T] apiguardian-api
│   ├── [D] mockito-junit-jupiter
│   ├── [T] mockito-core
│   ├── [T] byte-buddy
│   ├── [T] byte-buddy-agent
│   ├── [T] objenesis
│   ├── [D] hamcrest
```

## 4. Maven BOM configuration

```xml
<properties>
    <maven.compiler.release>17</maven.compiler.release>
    <spring.version>6.2.19</spring.version>
    <spring-security.version>6.5.11</spring-security.version>
</properties>
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.springframework</groupId>
            <artifactId>spring-framework-bom</artifactId>
            <version>${spring.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
        <dependency>
            <groupId>org.springframework.security</groupId>
            <artifactId>spring-security-bom</artifactId>
            <version>${spring-security.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```
BOMs manage versions; they do not introduce JARs.

## 5. Dependencies explicitly not required
| Dependency | Reason |
|---|---|
| spring-boot-* | Plain Spring |
| spring-security-oauth2-client | No OAuth2 client flow |
| spring-security-oauth2-authorization-server | Issuer out of scope |
| spring-data-jpa | No persistence |
| hibernate-core | No ORM |
| spring-jdbc | No database |
| spring-tx | No database transactions |
| spring-session-* | Stateless authentication |
| spring-data-redis | No distributed state |
| jjwt-* | Nimbus provides JWT |
| bcprov-* | JDK supports RS256 |
| Additional JWKS library | Nimbus supports JWKS |

## 6. Dependencies for JWKS, RS256 and certificates
| Capability | Providing dependency |
|---|---|
| JWT parsing | Nimbus |
| RS256 verification | Nimbus + JDK |
| RSA public key | JDK |
| PEM processing | Spring Security + JDK |
| JWK representation | Nimbus |
| JWKS retrieval | Spring Security + Nimbus |
| JWKS caching | Nimbus / Spring Security |
| X.509 processing | JDK |
| TLS communication | JDK / JSSE |
| Certificate truststore | JDK |
| Incoming HTTPS | Tomcat or gateway |

No additional Maven dependencies fundamentally required for remote JWKS.

## 7. Enterprise architecture decision
| Decision | Proposed baseline |
|---|---|
| Java | 17 |
| Framework | Plain Spring 6.2 |
| Security | Spring Security 6.5 |
| Packaging | WAR |
| Servlet container | External Tomcat |
| JWT engine | Nimbus |
| Cryptographic provider | JDK default |
| Persistence | None |
| Dependency management | Maven BOMs |

Alternative: executable JAR with embedded Tomcat. Verify actual POM, convergence, support and vulnerabilities.
```bash
mvn dependency:tree
mvn dependency:tree -Dscope=runtime
mvn dependency:analyze
mvn help:effective-pom
```
## Technical corrections and clarifications
Versions and dependency paths above are historical proposals, not verified resolved output. jcip-annotations and other transitives must be checked against exact versions. No live Maven verification was performed in this archive.

