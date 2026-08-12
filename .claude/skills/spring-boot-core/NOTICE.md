# Attribution

`references/` in this skill are vendored **verbatim** from
[jdubois/dr-jskill](https://github.com/jdubois/dr-jskill) — "Dr JSkill", Spring Boot best
practices by Julien Dubois — licensed under **Apache License 2.0**.

| Local file | Upstream path |
|---|---|
| `references/SPRING-BOOT-4.md` | `references/SPRING-BOOT-4.md` |
| `references/CONFIGURATION.md` | `references/CONFIGURATION.md` |
| `references/DATABASE.md` | `references/DATABASE.md` |
| `references/SECURITY.md` | `references/SECURITY.md` |
| `references/LOGGING.md` | `references/LOGGING.md` |
| `references/PROJECT-SETUP.md` | `references/PROJECT-SETUP.md` |
| `references/GIT.md` | `references/GIT.md` |

Vendored on 2026-08-12. `SKILL.md` is original work for this repository.

Upstream is Java + Maven oriented; this project is Kotlin + Gradle Kotlin DSL. Treat the
references as guidance to translate, not as snippets to paste.

To refresh:

```bash
git clone --depth 1 https://github.com/jdubois/dr-jskill.git /tmp/dr-jskill && cp /tmp/dr-jskill/references/{SPRING-BOOT-4,CONFIGURATION,DATABASE,SECURITY,LOGGING,PROJECT-SETUP,GIT}.md .claude/skills/spring-boot-core/references/
```
