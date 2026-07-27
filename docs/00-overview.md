# Enterprise Banking Platform

> A platform that mimics the banking platform(backend engineering focused)

# General Requirements

## Authentication

- JWT
- OAuth2
- Role-Based Access Control
- Refresh Tokens

## Account Management

- Create
- Close
- Freeze
- Unfreeze
- Balance
- Transactions

## Payments

- Checking
- Savings

### Handle

- Insufficient Funds
- duplicate requests
- rollback if something goes wrong

## Fraud Detection

event driven architecture

- payment - kafka - fraud detection - notification

## Notifications

- email
- sms
- push notifications
- different implementations behind one interface

## Reports

- Generate:
  - monthly
  - spending summaries
  - transaction history

## Tech Stack

Backend:

- java 21
- Spring Boot
- Spring Security
- Spring Data JPA
- Flyway(runs missing migrations)
- Hibernate

Database:

- PostgreSQL

Messaging:

- Kafka

Caching:

- Redis

Deployment:

- Docker
- Docker Compose

Cloud:

- AWS

Testing:

- JUnit
- Mockito
- Testcontainers

Documentation:

- OpenAPI/Swagger

Monitoring

- Prometheus
- Grafana

Logging

- ELK Stack

CI/CD

- GitHub Actions

# Real Production System

GitHub - GitHub Actions - Docker - Integration Tests - Build Image - Deploy - Health Checks - Metrics - Logs

# Engineering Practices

- DDD(Domain Driven Design)
- Clean Architecture
- SOLID
- Dependency Injection
- Repository Pattern
- Strategy Pattern
- Factory Pattern
- CQRS
- Event-driven Architecture

# Microservices

- Idempotency
  If a client accidentally sends the same payment request three times, ensure only one transfer is processed.

- Distributed Transactions
  Payment - Money withdrawal - Notification fails
  What happens?
  Should the transaction be rolled back?
  Should the notification be retried?
  Should the transaction be retried?

- Retry Logic
  Automatically retry with exponential backoff

- Caching
  Reduce unnecessary database queries

- Rate limiting
  Prevent abuse of endpoints

- Audit logs
  Banks never delete history
  Every action should be recorded

- Observability
  if a payment fails,
  - which service failed
  - which request caused it
  - how long did it take
  - how many retries were attempted

# The complete project I am for will have:

- 20–30 REST APIs
- 5–8 microservices
- PostgreSQL + Redis + Kafka
- Dockerized local development
- GitHub Actions CI/CD
- 90%+ test coverage for core business logic
- Architecture Decision Records (ADRs)
- OpenAPI documentation
- Monitoring dashboards
- Load testing with thousands of concurrent requests
- A clear README explaining the architecture and design tradeoffs
