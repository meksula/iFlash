# ADR-0002. Monorepo with a root aggregator POM

- Status: Accepted
- Date: 2026-11-25

## Context

iFlash consists of a matching engine library, several microservices and helper applications (broker
platform, trading simulator). It is a private learning project maintained by one person. Git
submodules were used earlier, which meant juggling many repositories and keeping their versions in
step by hand.

## Decision

We keep all modules in a single Git repository with a root Maven aggregator POM (`com.iflash:iflash`,
Java 25). Every service and library is a module of that POM, and CI builds the whole tree with
`mvn -B package`.

## Consequences

- One clone, one build, one pull request for changes that span several services.
- Cross-cutting documentation (ADRs, architecture diagrams, Kafka topic conventions) lives next to
  the code.
- Services are still deployed independently; the monorepo does not imply a shared release cycle.
- The CI build grows with every module. Selective builds can be introduced if it becomes too slow.
