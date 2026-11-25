# 0014. Container images with Jib, local Kubernetes loop with Skaffold

- Status: Accepted
- Date: 2026-11-25

## Context

Every microservice needs a container image for Docker Compose locally and for Kubernetes (minikube,
EKS). Writing and maintaining a Dockerfile per service, and requiring a Docker daemon on build
machines, adds work that does not teach anything new.

## Decision

- Images are built with Jib from Maven, without Dockerfiles or a Docker daemon, on the
  `eclipse-temurin:25-jre` base image.
- The local Kubernetes development loop will use Skaffold with one root `skaffold.yaml` and its `jib`
  builder (planned, not yet in the repository).

## Consequences

- Image builds are reproducible and layered (dependencies, resources, classes), so rebuilds are fast.
- Customising the image beyond what Jib supports (extra OS packages, custom entrypoints) is harder than
  with a Dockerfile.
- Skaffold and Jib integrate natively, so adding Skaffold later does not change how images are built.
