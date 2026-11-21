# 2026-11-21: POM fixes and Claude Code agent setup

## Outcome

- `helpers/trading-simulator/pom.xml`: added `<relativePath>../../pom.xml</relativePath>` to the parent and
  pinned `maven-compiler-plugin` to `3.11.0` (same as `iflash-engine`).
- `iflash-engine/pom.xml`: pinned `maven-surefire-plugin` to `3.5.4`.
- `mvn validate` passes without warnings for both modules. Committed by the owner in `a538c22`.
- Root `CLAUDE.md` added with a single line `@AGENTS.md`. Claude Code loads only `CLAUDE.md` on its own,
  so the import keeps `AGENTS.md` as the one source of agent rules.
- `.claude/` is versioned now; only `.claude/settings.local.json` stays git-ignored. The `/dev` skill
  moved from `skills/dev-SKILL.md` to `.claude/skills/dev/SKILL.md`, where Claude Code discovers it.

## Context cleanup

`docs/context/` holds fundamental knowledge only, not a to-do list:

- `open-items.md` removed, with its references in `AGENTS.md` and `AGENTS-README.md`. Known bugs belong
  in GitHub issues; the ones still present in code were moved to #19–#23.
- `project-overview.md`: removed the per-service State column and the feature backlog; the roadmap keeps
  the direction only. Recorded that `helpers/iflash-platform` stays on Spring Boot 3.5.6.
- `architecture-decisions.md`: the event timestamp entry states the rule, not the current inconsistency.
- `dev-environment.md`: removed the `mvn -N install` workaround.
