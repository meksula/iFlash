# 2026-11-21: Workstation migration and agent workflow

## Outcome

- iFlash development moves to a dedicated Windows 11 workstation with WSL2 (Ubuntu). Setup details are
  in `../dev-environment.md`.
- The toolchain is installed and verified: WSL2, Docker Desktop, Temurin 25.0.1, Maven 3.10, Git over
  SSH, IntelliJ opening the project from WSL, Claude Code in WSL.
- The repository was cloned from GitHub (`main` at `5a38435`, 2026-09-11). It is a monorepo; the
  earlier Git submodule layout no longer applies.
- First full build failed on the missing `relativePath` in `../../../helpers/trading-simulator` (see
  `../open-items.md`). Workaround `mvn -N install`; the fix itself is intentionally not committed yet.

## Agreed workflow between agents

- The design agent (Claude chat project) is used for brainstorming, decisions and documentation; Claude
  Code only implements.
- Knowledge is exchanged through `..` in this repository. The repository is the source of
  truth; the chat project may keep a read-only mirror of these files.
- The implementation agent is told about this in `../../../CLAUDE.md`, reads these files when it needs background,
  and does not edit them on its own.

## Findings from the repository review

All recorded in `../open-items.md`. Most important: unpushed work from the previous workstation (Kafka
in Compose and more), gateway route name `orderbook-service` vs `order-book-service`, and the
`instrument.initialized` event keyed by a random UUID, which defeats topic compaction.
