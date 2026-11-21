# Development environment

## Workstation

- Windows 11 Home, AMD Ryzen 9 7950X3D (16 cores / 32 threads), 32 GB RAM, 1 TB NVMe SSD.
- All development happens inside **WSL2, distribution `Ubuntu`** (24.04). The repository lives on the
  Linux filesystem at `~/development/iflash`, not under `/mnt/c`, because cross-filesystem access is
  several times slower.
- `%UserProfile%\.wslconfig` limits the VM to `memory=20GB`, `processors=24`, `swap=8GB`.

## Toolchain (inside WSL)

| Tool | Version | Installed via |
|---|---|---|
| JDK | Temurin 25.0.1 | SDKMAN (`~/.sdkman/candidates/java/25.0.1-tem`) |
| Maven | 3.10.x | SDKMAN |
| Docker | Docker Desktop with WSL2 backend and Ubuntu integration enabled | Windows installer |
| Git | Ubuntu package; SSH key for GitHub, `core.autocrlf=input` | apt |
| Claude Code | native installer | `curl -fsSL https://claude.ai/install.sh \| bash` |

Containers started from WSL publish ports to `localhost` on both Linux and Windows, so
`localhost:9092` (Kafka) or `localhost:5433` (PostgreSQL from `../../compose.yaml`) work from either side.

## IntelliJ IDEA

IntelliJ runs on Windows and opens the project from WSL:

- Project: `\\wsl.localhost\Ubuntu\home\meksula\development\iflash`
- JDK: a Linux JDK located in WSL (SDKMAN or one downloaded by IntelliJ into WSL). A Windows JDK
  must not be used for this project.
- Maven home: `\\wsl.localhost\Ubuntu\home\meksula\.sdkman\candidates\maven\<version>`
- Terminal: `wsl.exe -d Ubuntu`
- If indexing becomes slow, switch to Remote Development → WSL (IDE backend inside Linux).

## Building

```bash
mvn -T 1C -DskipTests package     # build all modules
mvn verify                        # with tests; Testcontainers uses Docker Desktop
```

Images are built by Jib per service (`mvn -pl <service> compile jib:dockerBuild`), then run with
`docker compose up`. `../../compose.yaml` mounts volumes from `${HOME}/Development/volumes/...` (capital
`D`); on Linux this is a different directory from `~/development`.