---
name: custom-project-rules
description: House rules for the ShopTourBoot / Voyage API repository — build and run commands, code style, git and commit conventions, security invariants, environment variables, and what must never be committed. Use at the start of any task in this repo, and before running builds, writing migrations, touching security config, or committing.
---

# ShopTourBoot House Rules

Project-specific conventions. When this file disagrees with a vendored reference in another
skill, **this file wins**.

## Commands

```bash
./gradlew build          # compile + test — the gate before "done"
```

```bash
./gradlew test           # tests only
```

```bash
./gradlew bootRun        # needs Postgres + Redis on localhost and VOYAGE_JWT_SECRET set
```

- Always use the **wrapper** (`./gradlew`), never a system `gradle`.
- To run against throwaway containers instead of local services, run `TestVoyageApplication`
  (it starts the app with `TestcontainersConfiguration`).
- Never add a `pom.xml` or Maven wrapper — this is a Gradle Kotlin DSL build.

## Environment

| Variable | Purpose |
|---|---|
| `VOYAGE_JWT_SECRET` | HS256 signing secret, **min 32 chars**. Required outside local dev. |

Local defaults in `application.yml` (Postgres `voyage/voyage@localhost:5432/voyage`,
Redis `localhost:6379`, mail `localhost:1025`) are for development only.

`local.properties` is developer-local — do not read secrets from it into committed files.

## Code style

- **Kotlin, tabs for indentation** — match the existing files exactly.
- Package root `com.shoptourr`; feature packages, not layer packages (see the `architecture` skill).
- Constructor injection; `val` and `data class` by default; no `!!`.
- `@ConfigurationProperties` data classes for config, not scattered `@Value`.
- Entities are `allOpen` via the Kotlin JPA plugin (`@Entity`, `@MappedSuperclass`,
  `@Embeddable`) — do not add `open` modifiers by hand.
- Compiler args `-Xjsr305=strict` and `-Xannotation-default-target=param-property` are set;
  do not remove them to silence a warning.

## Naming already established — follow it

- Tables and columns: `snake_case`, `UUID` PK, `TIMESTAMPTZ`, soft delete via `deleted_at`.
- Error codes: `SCREAMING_SNAKE_CASE` (`VALIDATION_ERROR`), `type` URI under
  `https://api.shoptourr.com/problems/...`.
- Config prefix for app settings: `voyage.*`.
- Correlation header: `X-Request-Id` (see `RequestIdFilter`).

## Security invariants

1. Stateless — `SessionCreationPolicy.STATELESS`, no server-side HTTP sessions.
2. Every new endpoint is authenticated **unless** added deliberately to the permit-list in
   `SecurityConfig`. Call it out in the change summary when you add one.
3. Passwords hashed with the injected `PasswordEncoder` (BCrypt). Never store or log a raw
   password, raw refresh token, or JWT. Refresh tokens are stored **hashed** (`token_hash`).
4. `server.error.include-message: never` stays — internal messages must not leak to clients.
5. Actuator exposure stays `health,info`.
6. Never commit a real secret, credential, or `.env`. If one is needed, use an env variable
   and document it in the table above.

## Database

- Every schema change is a new Flyway migration `src/main/resources/db/migration/V<N>__<name>.sql`.
- **Never edit an applied migration** and never renumber. `ddl-auto` stays `validate`.
- Never add a `data.sql`/`import.sql` path that competes with Flyway.

## Git

- Branch off `main`; do not commit directly to `main`.
- Conventional-commit style, matching history (`chore: bootstrap Voyage API foundation on Spring Boot 4`):
  `feat:`, `fix:`, `chore:`, `refactor:`, `test:`, `docs:`.
- Commit only when asked. Never `git push --force`, never rewrite pushed history,
  never `git checkout .`/`git reset --hard` over uncommitted work without confirmation.
- Do not commit `build/`, `.gradle/`, `.idea/`, `.kotlin/`, `local.properties`, or `.DS_Store`.

## Working agreements

1. **Run the build before saying it works.** Report the real output. If Docker is unavailable and
   Testcontainers tests skipped, say so — do not imply the integration layer was verified.
2. **Scope discipline** — no drive-by refactors, dependency bumps, or reformatting of files the
   task did not require. Unrelated problems get mentioned, not fixed silently.
3. **No new dependency without asking.** The Boot BOM manages versions; pin only what it does not.
4. **No speculative abstraction.** Build what the task needs; this codebase is young and small.
5. When a requirement is ambiguous in a way that changes the design, ask before building — but
   finish everything that does not depend on the answer first.

## Related skills

- `spring-boot-core` — framework conventions, configuration, security wiring
- `java-best-practices` — Kotlin/JVM code discipline, performance and concurrency
- `spring-boot-testing` — what to test and at which level
- `architecture` — boundaries, API contracts, schema and migration strategy, ADRs
