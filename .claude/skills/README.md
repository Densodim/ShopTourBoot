# .claude/skills

Project skills for ShopTourBoot (Voyage API). Claude Code loads each `SKILL.md` frontmatter at
session start and reads the body only when the description matches the task; the `references/`
files are read on demand from there.

| Skill | Covers | Vendored from |
|---|---|---|
| [spring-boot-core](spring-boot-core/SKILL.md) | Boot 4 conventions, config, Problem Details, Flyway/JPA/Redis wiring, security chain | [jdubois/dr-jskill](https://github.com/jdubois/dr-jskill) (Apache-2.0) |
| [java-best-practices](java-best-practices/SKILL.md) | Clean code, SOLID, patterns, JPA/perf smells, concurrency, logging — Kotlin-adapted | [decebals/claude-code-java](https://github.com/decebals/claude-code-java) + [affaan-m/everything-claude-code](https://github.com/affaan-m/everything-claude-code) (MIT) |
| [spring-boot-testing](spring-boot-testing/SKILL.md) | Test pyramid, slice tests, Testcontainers, MockMvc + security, definition of done | claude-code-java + everything-claude-code + dr-jskill |
| [architecture](architecture/SKILL.md) | Layering, API contracts, schema/migration strategy, observability, ADRs | [alirezarezvani/claude-skills](https://github.com/alirezarezvani/claude-skills) + claude-code-java + everything-claude-code (MIT) |
| [custom-project-rules](custom-project-rules/SKILL.md) | Commands, style, git, security invariants, working agreements | original |

Each folder's `NOTICE.md` records the exact upstream file, license, and a refresh command.

## Conventions

- Every `SKILL.md` starts with YAML frontmatter: `name` (matching the folder) and a `description`
  that states **what it covers and when to use it** — that sentence is the only thing Claude sees
  before deciding to load the skill, so keep it concrete.
- Vendored upstream files live under `references/` and are **not** named `SKILL.md`, so they are
  never registered as separate skills.
- Upstream references are Java + Maven oriented. This project is **Kotlin + Gradle Kotlin DSL** —
  their principles apply, their syntax does not.
- `custom-project-rules` overrides any vendored guidance that conflicts with it.

## Adding a skill

1. `mkdir .claude/skills/<name>` and write `SKILL.md` with the frontmatter above.
2. Put long-form material in `references/` and link it from a table in `SKILL.md`.
3. If it comes from a third party, add a `NOTICE.md` with source, license, and refresh command.
4. Add a row to the table above.
