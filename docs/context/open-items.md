
## Build

- **`../../helpers/trading-simulator/pom.xml` has no `<relativePath>` for its parent `com.iflash:iflash`.**
  Maven looks for `helpers/pom.xml`, which does not exist, then for the artifact in Maven Central, and
  fails. It only worked on machines where the root POM had been installed into `~/.m2` earlier.
  Workaround: `mvn -N install`. Fix: `<relativePath>../../pom.xml</relativePath>`.
- `../../iflash-engine/pom.xml` declares `maven-surefire-plugin` without a version (Maven warning, build is
  not reproducible).
- Spring Boot versions drift: most services `4.1.0`, `instrument-service` `4.1.1`,
  `../../helpers/iflash-platform` `3.5.6`.

## Local runtime (`../../compose.yaml`)

- No Kafka service, although `instrument-service` needs a broker at `localhost:9092`.
- Volumes are mounted from `${HOME}/Development/volumes/...` (capital `D`), while the project lives in
  `~/development`. Works, but creates a second, similarly named directory on Linux.
- No `SPRING_PROFILES_ACTIVE` is set, so `gateway-service` runs with its default `application.yml`,
  whose route URIs have no port (`http://rates-service`). That fits Kubernetes Services on port 80, not
  Compose, where containers listen on their own ports.
- Port 8090 is used by both `../../helpers/broker-platform` and Kafka UI (in the unpushed Compose setup); they
  cannot run on the host at the same time without remapping one of them.

## Gateway routing

- Routes point to `orderbook-service`, but the module, image and container are named
  `order-book-service`. DNS resolution will fail in Compose and in Kubernetes unless the Service is
  named `orderbook-service`.
- `application-local.yml` routes `/api/v1/broker` to port 8085, but `broker-management-service` sets no
  `server.port` (defaults to 8080). `matching-engine-service` and `settlement-service` set no port either.

## instrument-service

- `FinancialInstrumentInitializedEvent` is published as one message containing the whole catalogue,
  keyed by a random UUID per publication. On the compacted topic `instrument.initialized` this means
  compaction never replaces anything; every startup adds a new key. The decision was one record per
  instrument keyed by ticker (see `architecture-decisions.md`).
- The `RuntimeException` thrown inside `kafkaTemplate.send(...).whenComplete(...)` is never observed:
  nothing waits on the future returned by `whenComplete`, so a failed publication does not fail the
  startup.
- The event uses `Instant` for its timestamp while `TradeExecutedEvent` uses epoch millis; the
  convention needs to be fixed in an ADR.

## Documentation

- `../../helpers/broker-platform/README.md` still says Java 21 and suggests `postgres:18.6` on port 5432, while
  the project is on Java 25 and `compose.yaml` uses `postgres:17` on port 5433.
- The root `../../README.md` documents the public API as served today by the legacy `iflash-platform`.

## Unfinished threads

- No ADRs exist yet; `architecture-decisions.md` lists the candidates. The price display rules should
  become an ADR before `portfolio-service` is started.
- Claude Code agent definitions (`.claude/`) are git-ignored and must be copied manually between
  machines.
