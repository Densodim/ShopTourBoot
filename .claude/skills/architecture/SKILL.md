---
name: architecture
description: Architectural decisions for the Voyage API — layering and module boundaries, dependency direction, REST API contract design, database schema and migration strategy, observability and SLOs, and writing ADRs. Use when adding a new feature area, changing boundaries between packages, designing endpoints or tables, or deciding between competing designs.
---

# Architecture (ShopTourBoot / Voyage API)

Source material: [alirezarezvani/claude-skills](https://github.com/alirezarezvani/claude-skills),
[decebals/claude-code-java](https://github.com/decebals/claude-code-java) and
[affaan-m/everything-claude-code](https://github.com/affaan-m/everything-claude-code) (all MIT) —
see [NOTICE.md](NOTICE.md).

## What this service is

A stateless REST backend for [ShopTourr](https://github.com/Densodim/ShopTourr): Spring Boot 4.1
modular monolith, Postgres as the source of truth, Redis for ephemeral state, first-party HS256
JWT auth, Flyway-owned schema. It is deliberately **not** microservices and **not** reactive.
Any proposal to change either of those is an ADR-level decision, not an implementation detail.

## Layering and dependency direction

```
web  →  service (domain)  →  repository  →  Postgres / Redis
 ▲            ▲
 DTOs      entities, domain rules
```

- Dependencies point **inward only**. `web` may know about `service`; `service` must not import
  anything from `web` (no `HttpServletRequest`, no `ResponseEntity`, no controller DTOs).
- **Entities stop at the service boundary.** Mapping to DTOs happens in the service or a dedicated
  mapper, never in the controller and never by returning an entity from a `@RestController`.
- Cross-feature calls go **service → service**, never repository → other feature's repository.
- Shared infrastructure lives in `config/`; shared web plumbing (filters, advice) in `web/`.

## Package structure as the app grows

Organize by **feature**, not by technical layer:

```
com.shoptourr
  config/            cross-cutting Spring config
  web/               shared web plumbing (filters, @RestControllerAdvice)
  identity/          user registration, login, refresh tokens
    IdentityController.kt   (web-facing, thin)
    IdentityService.kt      (transaction boundary, business rules)
    AppUser.kt              (entity)
    AppUserRepository.kt
  <next-feature>/    same shape
```

A feature package owns its entities and repositories. If two features need the same table,
that is a signal to extract a third feature package that owns it — not to share repositories.

## API contract rules

- Base path `/api`, plural resource nouns, kebab-case paths, no verbs in URLs.
- Correct status codes: 201 + `Location` on create, 204 on delete, 400 validation,
  401 unauthenticated, 403 unauthorized, 404 missing, 409 conflict, 422 semantic failure.
- All errors are RFC 9457 `ProblemDetail` with a stable machine-readable `code`.
  Adding a new error code is part of the API contract — keep codes stable once shipped.
- Lists are paginated and their default page size documented.
- Mutating endpoints that clients may retry accept `Idempotency-Key` — the `idempotency_record`
  table exists for exactly this; use it rather than inventing a parallel mechanism.
- Breaking a shipped contract requires a version or a deprecation window, not a silent change.
- Every endpoint appears in the OpenAPI document; keep springdoc annotations accurate.

## Data and schema

- Postgres is the source of truth; **Redis is disposable** — never store anything whose loss
  would corrupt state (it holds caches, rate-limit counters, short-lived tokens).
- Schema conventions already set in `V1__init_identity.sql`, keep them: `UUID` primary keys,
  `snake_case` names, `TIMESTAMPTZ` for time, soft delete via `deleted_at`, partial unique
  indexes for "unique among live rows", hashed secrets (`token_hash`) never raw tokens.
- Migrations are **forward-only and immutable once applied**. Sequential `V<N>__<name>.sql`.
- Expand/contract for breaking changes: add nullable column → backfill → start writing →
  switch reads → drop old column in a later release. Never combine those in one migration.
- Index every foreign key and every column used in a `WHERE` on a growing table.

## Observability

- Structured logs with the `requestId` MDC from `RequestIdFilter` on every request.
- Actuator exposes `health,info` only; liveness/readiness probes are enabled.
- Before adding a metric or trace, define what decision it informs. Define SLOs
  (availability, p99 latency) per user-facing endpoint group rather than per handler.

## Decision records

Non-obvious or hard-to-reverse choices get an ADR in `docs/adr/NNNN-title.md`
(context → decision → consequences → alternatives rejected). Examples that warrant one:
auth model changes, adding a message broker or a second datastore, switching to WebFlux,
splitting the monolith, changing the public error contract.

Read `references/architecture-decision-records.md` for the template.

## Review questions for any design

1. What is the dependency direction, and does anything point outward?
2. What happens on partial failure — is the operation idempotent or retry-safe?
3. What is the transaction boundary, and is it the smallest one that is correct?
4. Which parts are hard to reverse? Those need the most scrutiny now.
5. What does this cost in schema, latency, and operational surface — and is it worth it?
6. Is this the simplest thing that satisfies the requirement, or speculative generality?

## References (load on demand)

| File | Read when | Source |
|---|---|---|
| [references/architecture-review.md](references/architecture-review.md) | Reviewing structure of existing code, layering violations | claude-code-java |
| [references/hexagonal-architecture.md](references/hexagonal-architecture.md) | Considering ports/adapters for a feature | ECC |
| [references/api-design.md](references/api-design.md) | Designing new endpoints | ECC |
| [references/api-design-reviewer.md](references/api-design-reviewer.md) | Reviewing an API contract | alirezarezvani |
| [references/database-schema-designer.md](references/database-schema-designer.md) | Designing new tables and relationships | alirezarezvani |
| [references/migration-architect.md](references/migration-architect.md) | Planning a risky or multi-step migration | alirezarezvani |
| [references/observability-designer.md](references/observability-designer.md) | Adding metrics, traces, structured logs | alirezarezvani |
| [references/slo-architect.md](references/slo-architect.md) | Defining SLIs/SLOs and error budgets | alirezarezvani |
| [references/architecture-decision-records.md](references/architecture-decision-records.md) | Writing an ADR | ECC |
