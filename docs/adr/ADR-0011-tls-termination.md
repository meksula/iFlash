# ADR-0011. TLS termination

- Status: Accepted
- Date: 2026-11-25

## Context

Traffic from the internet to the cluster must be encrypted. On AWS the usual default is an Application
Load Balancer that terminates TLS, with plain HTTP inside the VPC. The project is also a learning
vehicle for certificate management and L4 versus L7 load balancing.

## Decision

- An ALB terminating TLS with plain HTTP inside the VPC is recognised as the standard, acceptable
  production default.
- For learning purposes iFlash follows a different path: an NLB in TCP pass-through mode, with Envoy
  terminating TLS. Certificates are issued by cert-manager from Let's Encrypt and delivered to Envoy
  over SDS (SNI routing, rotation without restart).
- AWS App Mesh is not used, because it is being retired.

## Consequences

- More moving parts than an ALB (Envoy, cert-manager, SDS), accepted as the learning goal.
- Certificate rotation happens inside the cluster without restarts.
- Switching to the ALB default later only changes the edge; services are unaffected.
