---
name: java-best-practices
description: JVM code discipline for this Kotlin/Java Spring Boot service — clean code, SOLID, design patterns, null and immutability handling, JPA/N+1 performance smells, concurrency with virtual threads, logging hygiene, and code-review checklists. Use when writing, refactoring, or reviewing Kotlin or Java code in this repo.
---

# JVM Best Practices (Kotlin-first)

Source material: [decebals/claude-code-java](https://github.com/decebals/claude-code-java) (MIT)
plus Kotlin material from [affaan-m/everything-claude-code](https://github.com/affaan-m/everything-claude-code) (MIT) — see [NOTICE.md](NOTICE.md).

> **This codebase is Kotlin, not Java.** The Java references still apply at the level of
> principles (naming, cohesion, SOLID, JPA behaviour, concurrency semantics), but their syntax
> does not. Every Java example must be re-expressed in idiomatic Kotlin before it lands here.
> Java-only idioms that Kotlin replaces: getters/setters and Lombok (use `data class` /
> properties), `Optional<T>` in domain code (use `T?`), `Objects.requireNonNull` (use the type
> system), builder classes for simple objects (use named + default arguments).

## Kotlin rules for this repo

1. **Immutability by default.** `val` over `var`; `data class` for DTOs and value objects;
   `List`/`Map` (read-only) over `MutableList`/`MutableMap` in public signatures.
2. **Null safety is the design.** No `!!`. Model absence with `T?` and handle it at the boundary
   (`?:`, `let`, `requireNotNull` with a message). Platform types from Java APIs get an explicit
   nullable annotation or an early check — `-Xjsr305=strict` is on, keep it honest.
3. **Expression bodies for one-liners**, block bodies once there is branching.
4. **Explicit return types on public API.** Inference is fine for private/local declarations.
5. **No `object`/companion singletons holding mutable state.** Constructor-inject Spring beans
   instead; `companion object` is for constants (as in `RequestIdFilter.HEADER`).
6. **Extension functions over utility classes**, scoped to the package that needs them.
7. **Exceptions are typed and meaningful.** Define domain exceptions, map them in
   `web/ApiExceptionHandler.kt`; never catch `Exception` to swallow it, never `printStackTrace()`.
8. **Tabs for indentation**, matching the existing sources; keep the existing formatting style.

## Naming and structure

- Intention-revealing names; no abbreviations (`req`, `usr`, `tmp`) and no type suffixes in
  variables. Boolean-returning functions read as predicates (`isExpired`, `hasAccess`).
- Functions do one thing; extract when a function needs a comment to explain a block.
- Keep classes cohesive — if a class needs three "and"s to describe, split it.
- Prefer a small explicit dependency list; a constructor with 5+ collaborators means the class
  is doing too much.

## Performance smells to watch here specifically

- **N+1 queries** — the single most common failure in this stack. Use fetch joins or
  `@EntityGraph`; verify by enabling SQL logging in a test, not by assumption.
- **Unbounded queries** — every list endpoint and repository method that can grow is paginated.
- **Entity leakage** — entities never cross into the web layer; map to DTOs inside the service.
- **String concatenation in loops / logging** — use SLF4J placeholders (`log.debug("id={}", id)`),
  never string templates in log calls on hot paths.
- **Transaction scope** — `@Transactional` on service methods, as narrow as possible; never on
  controllers. `readOnly = true` for queries.

## Concurrency

Virtual threads are enabled (`spring.threads.virtual.enabled: true`).

- Blocking JDBC/HTTP calls are fine on virtual threads — do **not** convert the codebase to
  reactive to "fix" blocking.
- **No `synchronized` around blocking calls** (it pins the carrier thread); use `ReentrantLock`
  if you truly need mutual exclusion.
- No thread-local caching assumptions; virtual threads are not pooled. MDC via `RequestIdFilter`
  is set and cleared per request — keep the `finally` cleanup on any similar filter.
- Prefer immutable shared state over locks. If you need coroutines, read
  `references/kotlin-coroutines-flows.md` first and keep them out of the request path unless
  there is a measured reason.

## Logging

- One logger per class, SLF4J, parameterized messages.
- Never log secrets, JWTs, password hashes, or full request bodies containing credentials.
- `DEBUG` for developer detail, `INFO` for lifecycle/business events, `WARN` for recoverable
  anomalies, `ERROR` only with an exception attached and an actionable message.

## References (load on demand)

| File | Read when | Source |
|---|---|---|
| [references/kotlin-patterns.md](references/kotlin-patterns.md) | Idiomatic Kotlin: sealed classes, scope functions, result types | ECC |
| [references/clean-code.md](references/clean-code.md) | DRY/KISS/YAGNI, naming, function design, refactoring | claude-code-java |
| [references/solid-principles.md](references/solid-principles.md) | Responsibility/abstraction problems, dependency direction | claude-code-java |
| [references/design-patterns.md](references/design-patterns.md) | Choosing a pattern (and when not to) | claude-code-java |
| [references/performance-smell-detection.md](references/performance-smell-detection.md) | Hunting slow code, allocation and query smells | claude-code-java |
| [references/jpa-patterns.md](references/jpa-patterns.md) | Entity mapping, fetch strategy, N+1, transactions | claude-code-java |
| [references/concurrency-review.md](references/concurrency-review.md) | Shared state, thread-safety review | claude-code-java |
| [references/kotlin-coroutines-flows.md](references/kotlin-coroutines-flows.md) | Only if introducing coroutines/Flow | ECC |
| [references/logging-patterns.md](references/logging-patterns.md) | Structured logging, MDC, log-level policy | claude-code-java |
| [references/java-code-review.md](references/java-code-review.md) | Running a review pass over a diff | claude-code-java |
| [references/security-audit.md](references/security-audit.md) | Auditing auth, input handling, secrets, injection | claude-code-java |
