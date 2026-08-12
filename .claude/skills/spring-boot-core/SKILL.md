---
name: spring-boot-core
description: Spring Boot 4.x conventions for this Voyage API service — application structure, configuration and profiles, Problem Details, Actuator, JPA/Flyway/Postgres wiring, Redis, security filter chain, logging. Use when adding or changing Spring beans, controllers, configuration properties, application.yml, migrations, or the security setup.
---

# Spring Boot Core (ShopTourBoot / Voyage API)

Baseline rules for backend work in this repository. Source material: [jdubois/dr-jskill](https://github.com/jdubois/dr-jskill) (Apache-2.0), adapted for this project's stack — see [NOTICE.md](NOTICE.md).

## This project's stack — do not drift from it

| Aspect | Fact |
|---|---|
| Build | Gradle **Kotlin DSL** (`build.gradle.kts`), wrapper `./gradlew` |
| Language | **Kotlin** 2.3 on JVM toolchain **21** |
| Framework | Spring Boot **4.1**, `spring-boot-starter-webmvc` (servlet, not WebFlux) |
| Threads | Virtual threads enabled (`spring.threads.virtual.enabled: true`) |
| Persistence | JPA/Hibernate + **Flyway** + PostgreSQL, `ddl-auto: validate`, `open-in-view: false` |
| Cache/session | Redis (`spring-boot-starter-data-redis`) |
| Security | Spring Security + **OAuth2 Resource Server** with first-party HS256 JWT |
| Errors | RFC 9457 `ProblemDetail` (`spring.mvc.problemdetails.enabled: true`) |
| Docs | springdoc-openapi 3.x → `/v3/api-docs`, `/swagger-ui.html` |
| Tests | JUnit 5 + Testcontainers (Postgres 16, Redis 7) |

> The bundled references are written for **Java + Maven**. Translate every snippet to
> **Kotlin + Gradle Kotlin DSL** before applying it here. Never introduce a `pom.xml`.

## Non-negotiable rules

1. **Constructor injection only.** No `@Autowired` on fields or setters. In Kotlin that means
   primary-constructor parameters (`class FooService(private val repo: FooRepository)`).
2. **No new starter or dependency without asking.** Dependencies are pinned by the Spring Boot BOM;
   add versions only where the BOM does not manage them (as with springdoc).
3. **Schema changes go through Flyway.** `ddl-auto` stays `validate`. Add
   `src/main/resources/db/migration/V<N>__<snake_case>.sql`; never edit an applied migration.
4. **`open-in-view` stays false.** Load what the response needs inside the transactional service —
   no lazy-loading in the web layer.
5. **Errors are `ProblemDetail`.** Extend `web/ApiExceptionHandler.kt`; keep `type`, `title`,
   `code` and (for validation) the `errors` array shape already established there.
6. **Secrets come from the environment.** `voyage.jwt.secret` must resolve from `VOYAGE_JWT_SECRET`
   in any non-dev environment. Never commit a real secret or log a token.
7. **Configuration is typed.** New config → an `@ConfigurationProperties` data class under
   `config/` (see `JwtProperties.kt`), registered via `@EnableConfigurationProperties`.
   No scattered `@Value` strings.
8. **Public endpoints are explicit.** Anything not listed in `SecurityConfig.securityFilterChain`
   requires authentication. When you add a public route, add it to that allow-list deliberately
   and say so in the change description.
9. **Actuator exposure stays minimal** — `health,info` only.
10. **Every request keeps its correlation id.** `RequestIdFilter` puts `requestId` in the MDC and
    echoes `X-Request-Id`; log through SLF4J with placeholders so the id stays attached.

## Package layout (`com.shoptourr`)

```
config/   Spring configuration + @ConfigurationProperties (SecurityConfig, JwtConfig, JacksonConfig)
web/      REST controllers, filters, @RestControllerAdvice   ← DTOs only, no entities
<domain>/ feature packages (service + repository + entity) as the app grows
```

Add features as **domain packages**, not as global `service/`, `repository/`, `entity/` buckets.
Controllers stay thin: validate input, call a service, map to a response DTO.

## Writing a controller

- `@RestController` + `@RequestMapping("/api/...")`, one resource per class.
- Request bodies are `data class` DTOs with `jakarta.validation` annotations; validate with `@Valid`.
- Never accept or return a JPA entity from a controller.
- Return `ResponseEntity<T>` when status or headers vary; a plain body otherwise.
- Paginate list endpoints (`Pageable`) instead of returning unbounded collections.
- Document with springdoc annotations when the signature is not self-explanatory.

## Configuration and profiles

- `application.yml` holds dev-safe defaults; environment variables override in deployment.
- Use `${VAR:default}` only where the default is genuinely safe for local development.
- Profile-specific overrides go in `application-<profile>.yml`, not in code branches.

## References (load on demand)

| File | Read when |
|---|---|
| [references/SPRING-BOOT-4.md](references/SPRING-BOOT-4.md) | Boot 4 specifics, changes from Boot 3, migration traps |
| [references/CONFIGURATION.md](references/CONFIGURATION.md) | Profiles, externalized config, `@ConfigurationProperties`, secrets |
| [references/DATABASE.md](references/DATABASE.md) | Datasource/Hibernate tuning, Testcontainers, connection pooling |
| [references/SECURITY.md](references/SECURITY.md) | Filter chains, JWT, authorization, CORS, security checklist |
| [references/LOGGING.md](references/LOGGING.md) | SLF4J usage, structured logging, MDC, log levels |
| [references/PROJECT-SETUP.md](references/PROJECT-SETUP.md) | Project layout and tooling conventions |
| [references/GIT.md](references/GIT.md) | Commit and branching conventions |

## Verify before claiming done

```bash
./gradlew build
```

Run it and report the real result. If Docker is unavailable, Testcontainers-backed tests skip —
say so explicitly rather than implying full coverage ran.
