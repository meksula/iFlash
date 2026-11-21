# Shared context

This directory is how knowledge moves between the two kinds of agents working on iFlash:

- **Architect / design agent**: a long-running Claude chat project used for brainstorming, architecture
  decisions, design reviews and documentation. It does not run code.
- **Implementation agent**: Claude Code running in the repository. It implements ideas, runs builds and
  tests, and does not take part in the design discussions.

The implementation agent cannot read the design conversations, so their outcome is written down here.

## Files

| File | What it holds |
|---|---|
| [project-overview.md](project-overview.md) | Purpose, architecture map, modules, tech stack, current state, roadmap |
| [architecture-decisions.md](architecture-decisions.md) | Decisions made so far and the reasoning behind them |
| [dev-environment.md](dev-environment.md) | Workstation and toolchain setup, IDE configuration, known environment gotchas |
| [open-items.md](open-items.md) | Known issues and inconsistencies found in the code, and unfinished threads |
| [sessions/](sessions) | Short summaries of individual design sessions, newest last |

## Rules

1. **The repository is the source of truth.** If code and these files disagree, the code wins and the
   files are out of date.
2. **Decisions are changed by the owner or the design agent**, not by the implementation agent. If an
   implementation changes something described here, mention it in the summary of the work instead of
   editing these files.
3. **Not mandatory reading.** Read the relevant file when you need background: why something is built
   the way it is, what the project is aiming for, or what is already known to be broken.
4. Formal ADRs, once introduced, will live in `docs/adr/` and take precedence over
   `architecture-decisions.md`.

Last updated: 2026-11-21
