# 0010. Kubernetes-native service discovery and gateway routing

- Status: Accepted
- Date: 2026-11-25

## Context

Clients (broker platform, trading simulator) need one entry point, and services need to find each
other. Spring Cloud offers Eureka for discovery, while Kubernetes already provides DNS-based service
discovery. The project targets minikube locally and EKS in the cloud.

## Decision

- No Eureka. Services are addressed by plain Kubernetes DNS names with ports, for example
  `http://rates-service:8081`, resolved by CoreDNS. The same configuration works in minikube and EKS.
- `gateway-service` (Spring Cloud Gateway) is the single entry point for `/api/v1/**`. Routes are
  configured under `spring.cloud.gateway.server.webflux`; URIs include the `http://` scheme; a
  predicate can list several paths and URI variables (`{ticker}`). Predicates decide matching and are
  combined with AND; filters do the modification.
- The `local` profile of the gateway points to `localhost` ports for running services outside a
  cluster.
- In general we prefer Kubernetes-native mechanisms over Spring-specific abstractions where they match
  what production uses.

## Consequences

- One less component to run, and local and cloud environments behave the same way.
- Client-side load balancing and registry metadata from Eureka are not available; Kubernetes Services
  do the load balancing.
- Running outside Kubernetes needs the `local` profile or equivalent host entries.
