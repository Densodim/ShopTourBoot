---
name: spring-boot-testing
description: Testing strategy for this Kotlin Spring Boot service — the test pyramid, slice tests (@WebMvcTest, @DataJpaTest), Testcontainers with @ServiceConnection, MockMvc and security testing, JWT test setup, and what "verified" means before reporting done. Use when writing, fixing, or reviewing tests, or when asked whether a change is proven to work.
---

# Spring Boot Testing (ShopTourBoot / Voyage API)

Source material: [decebals/claude-code-java](https://github.com/decebals/claude-code-java),
[affaan-m/everything-claude-code](https://github.com/affaan-m/everything-claude-code) (both MIT)
and [jdubois/dr-jskill](https://github.com/jdubois/dr-jskill) (Apache-2.0) — see [NOTICE.md](NOTICE.md).

## What this project already has

- **JUnit 5** + `kotlin-test-junit5`, run through `./gradlew test` (`useJUnitPlatform()`).
- **Boot 4 modular test starters** — `spring-boot-starter-webmvc-test`,
  `-data-jpa-test`, `-data-redis-test`, `-security-test`, `-validation-test`, `-flyway-test`,
  `-actuator-test`, `-mail-test`, `-security-oauth2-resource-server-test`.
  Boot 4 split the old fat `spring-boot-starter-test`; add the **slice-specific** starter you need
  rather than reaching for a general one.
- `TestcontainersConfiguration` — Postgres 16 + Redis 7 wired with `@ServiceConnection`
  (no manual datasource properties).
- `TestVoyageApplication` — `fromApplication<VoyageApplication>().with(TestcontainersConfiguration::class)`
  to run the app locally against throwaway containers.
- `VoyageApplicationTests` — `@SpringBootTest` + `@Testcontainers(disabledWithoutDocker = true)`,
  with `@DynamicPropertySource` supplying a test `voyage.jwt.secret`.

Follow these patterns; do not invent a second bootstrap mechanism.

## The pyramid — pick the cheapest test that proves the thing

| Level | Use | Annotations |
|---|---|---|
| **Unit** (most) | Pure logic, mappers, validators, config classes | none — plain classes, e.g. `JwtConfigTest` |
| **Slice** (some) | One layer with real Spring wiring | `@WebMvcTest`, `@DataJpaTest`, `@JsonTest`, `@DataRedisTest` |
| **Integration** (few) | Wiring across the whole app, migrations, real DB behaviour | `@SpringBootTest` + `@Import(TestcontainersConfiguration::class)` |

A `@SpringBootTest` for something a unit test could cover is a defect in the test, not thoroughness.

## Rules

1. **Test behaviour, not implementation.** Assert on outcomes and contracts, never on the number
   of times a private collaborator was called unless the interaction *is* the requirement.
2. **Names describe the case.** Kotlin backticks, as in the existing tests:
   `` fun `rejects login when refresh token is revoked`() ``.
3. **Arrange–Act–Assert**, visually separated.
4. **One logical assertion per test.** Multiple `assertEquals` on one returned object is fine;
   testing two behaviours in one test is not.
5. **No conditionals or loops in test bodies** — use `@ParameterizedTest`.
6. **Deterministic.** No `Thread.sleep`, no dependence on wall-clock now (inject a `Clock`),
   no test order dependence, no shared mutable state between tests.
7. **Every integration test that needs Docker carries `@Testcontainers(disabledWithoutDocker = true)`**
   so the suite still runs on a Docker-less machine.
8. **Never disable a failing test to make the build green.** Fix it or report it.
9. **Bug fixes start with a failing test** that reproduces the bug, then the fix.

## Web layer

- `@WebMvcTest(FooController::class)` + `MockMvc`, service collaborators as `@MockitoBean`.
- Security is on in these slices: use `spring-security-test`
  (`SecurityMockMvcRequestPostProcessors.jwt()`) to authenticate, and add an explicit
  **401/403 test** for every protected endpoint.
- Assert the error contract too: status, `Content-Type: application/problem+json`, and the
  `code` / `errors` fields produced by `ApiExceptionHandler`.

## Persistence layer

- `@DataJpaTest` + `@Import(TestcontainersConfiguration::class)` — test against **real Postgres**,
  never H2. Dialect differences (partial unique indexes, `TIMESTAMPTZ`, `UUID`) are exactly what
  breaks in production.
- Flyway runs in tests; a migration that does not apply cleanly is a failing test.
- To catch N+1: assert on statement counts or enable `hibernate.show_sql` in the test profile,
  then fix with a fetch join or `@EntityGraph`.

## JWT / security tests

- Supply `voyage.jwt.secret` via `@DynamicPropertySource` (≥32 chars) — never read the dev
  default and never commit a production secret into a test.
- `JwtConfigTest` is the model for round-trip encoder/decoder tests.

## Definition of done

```bash
./gradlew test
```

Report the actual outcome. If Docker was unavailable and container-backed tests were skipped,
say that explicitly — "tests pass" while the integration layer never ran is a false report.

## References (load on demand)

| File | Read when | Source |
|---|---|---|
| [references/springboot-tdd.md](references/springboot-tdd.md) | Red-green-refactor loop for a Spring feature | ECC |
| [references/test-quality.md](references/test-quality.md) | Judging whether existing tests are worth anything | claude-code-java |
| [references/kotlin-testing.md](references/kotlin-testing.md) | Kotlin-specific test idioms, mocking, coroutine tests | ECC |
| [references/dr-jskill-TEST.md](references/dr-jskill-TEST.md) | Testcontainers + `@ServiceConnection`, integration-test layout | dr-jskill |
| [references/springboot-verification.md](references/springboot-verification.md) | Pre-merge verification checklist | ECC |
