# 0001. Record architecture decisions as ADRs in the repository

- Status: Accepted
- Date: 2026-11-25

## Context

Architecture is discussed in a separate design agent and its outcome has so far been collected in
`docs/context/architecture-decisions.md`. That file is a flat list without status, date or history,
so it is hard to tell which decisions are settled, which are open and which were replaced.

GitHub Wiki was considered for documentation and rejected: wiki changes do not go through pull request
review, are not versioned together with the code and fit ADRs poorly.

## Decision

We record significant architecture decisions as Architecture Decision Records in `docs/adr/`, using
the Michael Nygard format (Context, Decision, Consequences) with a status and a date.

- Files are named `NNNN-kebab-case-title.md` and numbered sequentially. Numbers are never reused.
- An accepted ADR is not rewritten. A changed decision gets a new ADR that supersedes the old one, and
  the old one's status is set to `Superseded by NNNN`.
- New ADRs start from [template.md](template.md) and are added to the index in [README.md](README.md).
- Documentation is kept as code (Markdown and Mermaid in the repository). Publishing with MkDocs
  Material or Docusaurus on GitHub Pages is optional and can be added later.
- ADRs take precedence over `docs/context/architecture-decisions.md`, which remains the place for
  design notes that are not yet decisions.

## Consequences

- Decisions are reviewed in pull requests and versioned with the code they describe.
- Implementation agents have one authoritative place to check why the system is built the way it is.
- Keeping ADRs in sync with the code is a manual responsibility; when the code and an ADR disagree,
  either the code is wrong or a superseding ADR is missing.
