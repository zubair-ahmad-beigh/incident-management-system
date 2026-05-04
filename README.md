# Incident Management System (IMS) — Backend

Production-grade, reactive incident management backend modeled after PagerDuty / Datadog.  
Handles **10,000 signals/sec** with full async processing, deduplication, RCA-gated lifecycle, and built-in observability.

---

## Tech Stack

| Layer           | Technology                              |
|-----------------|-----------------------------------------|
| Runtime         | Java 17, Spring Boot 3.2               |
| API             | Spring WebFlux (non-blocking)           |
| Message Bus     | Apache Kafka (12 partitions)            |
| Source of Truth | MySQL 8.0 + R2DBC (reactive)            |
| Raw Signals     | MongoDB 7 (reactive)                    |
| Cache           | Redis 7 (reactive, Lettuce pool)        |
| Resilience      | Resilience4j (Circuit Breaker + Retry)  |
| Rate Limiting   | Bucket4j (token bucket, per-IP)         |
| Migrations      | Flyway                                  |
| Metrics         | Micrometer + Prometheus                 |
| Containers      | Docker + Docker Compose                 |

---

## Architecture Overview

```
                    ┌─────────────────────────────────────────────────────┐
                    │              IMS Backend (Spring WebFlux)            │
                    │                                                       │
  POST /api/signals │  ┌────────────┐   Kafka    ┌──────────────────────┐ │
  ──────────────────┼─►│  Signal    ├──────────►│  Signal Consumer     │ │
  (10k req/sec)     │  │  Controller│  Publish   │  (12 threads)        │ │
                    │  └────────────┘            └─────────┬────────────┘ │
                    │       │                              │               │
                    │  Rate Limiter (Bucket4j)    ┌────────▼────────┐     │
                    │                             │  Debounce Logic │     │
                    │                             │  (10s window)   │     │
                    │                             └────────┬────────┘     │
                    │                                      │               │
                    │                         ┌────────────▼────────────┐ │
                    │                         │  MySQL      (Incidents)  │ │
                    │  ┌────────────────┐     │  MongoDB   (Signals)    │ │
  GET/PATCH APIs   ─┼─►│IncidentService │◄───►│  Redis     (Cache)      │ │
                    │  │ RcaService     │     └─────────────────────────┘ │
                    │  │ DashboardSvc   │                                  │
                    │  └────────────────┘                                  │
                    └─────────────────────────────────────────────────────┘
```

### Design Patterns Used

| Pattern          | Where Applied                                        |
|------------------|------------------------------------------------------|
| **State**        | `IncidentState` → enforces OPEN→INVESTIGATING→RESOLVED→CLOSED |
| **Strategy**     | `AlertStrategy` → pluggable severity routing per signal type |
| **Cache-Aside**  | `IncidentService` → Redis L1, MySQL source of truth |
| **Circuit Breaker** | `@CircuitBreaker` on all DB calls via Resilience4j |
| **Retry**        | `@Retry` with exponential backoff for transient DB errors |
| **Token Bucket** | `RateLimiter` → per-IP 10k/sec with 20k burst       |

---

## Quick Start

### 1. Start all infrastructure + backend

```bash
docker compose up -d
```

Services started:

| Service      | Port  | URL                              |
|--------------|-------|----------------------------------|
| IMS Backend  | 8080  | http://localhost:8080            |
| Kafka UI     | 8090  | http://localhost:8090            |
| MySQL        | 3306  |                                  |
| MongoDB      | 27017 |                                  |
| Redis        | 6379  |                                  |

### 2. Verify health

```bash
curl http://localhost:8080/actuator/health | jq .
```

### 3. Run unit tests only (no Docker needed)

```bash
cd backend
mvn test
```

### 4. Build & run locally (requires infra running)

```bash
cd backend
mvn spring-boot:run
```

---

## API Reference

### Signal Ingestion

```
POST /api/signals
Content-Type: application/json

{
  "componentId": "order-service",
  "type":        "DB_FAILURE",
  "message":     "Connection pool exhausted",
  "timestamp":   "2024-06-15T10:30:00Z"
}

→ 202 Accepted
{ "status": "accepted", "signalId": "uuid", "message": "Signal queued for async processing" }
```

**Signal types and severity mapping:**

| Signal Type               | Severity |
|---------------------------|----------|
| `DB_FAILURE`              | **P0**   |
| `DATABASE_ERROR`          | **P0**   |
| `CONNECTION_POOL_EXHAUSTED` | **P0** |
| `NETWORK_ERROR`           | **P1**   |
| `TIMEOUT`                 | **P1**   |
| `SERVICE_UNAVAILABLE`     | **P1**   |
| `CACHE_MISS`              | **P2**   |
| `REDIS_ERROR`             | **P2**   |
| *(any other)*             | **P2**   |

---

### Incident Lifecycle

```
GET    /api/incidents/{id}         → single incident (Redis-first)
GET    /api/incidents?status=OPEN  → list by status
GET    /api/incidents?component=x  → list by component
PATCH  /api/incidents/{id}/status  → state transition
```

**Valid transitions:**

```
OPEN → INVESTIGATING → RESOLVED → CLOSED
                                  ↑
                              Requires valid RCA
```

Attempting an invalid transition returns `HTTP 409 Conflict`.

---

### RCA Management

```
POST /api/rca
{
  "incidentId":       "uuid",
  "rootCause":        "Non-empty string",
  "fixApplied":       "Non-empty string",
  "preventionSteps":  "Non-empty string"
}
→ 201 Created

GET /api/rca/{incidentId}   → retrieve RCA
```

All three fields are mandatory. Blank/whitespace-only values → `HTTP 422 Unprocessable Entity`.

---

### Dashboard

```
GET /api/dashboard/summary
→ {
    "openIncidents":          3,
    "investigatingIncidents": 1,
    "resolvedIncidents":      12,
    "closedIncidents":        47,
    "totalSignalsIngested":   125000,
    "incidentsCachedInRedis": 4
  }
```

---

### Observability

```
GET /actuator/health      → health + component status
GET /actuator/prometheus  → Prometheus metrics (connect Grafana)
GET /actuator/metrics     → all Micrometer metrics
```

**Key metrics:**
- `ims_signals_accepted_total` — total ingested signals
- `ims_signals_rejected_total` — rate-limited/failed signals
- `ims_incidents_active_count` — gauge of non-CLOSED incidents
- Throughput logged every 5 seconds: `signals/sec + avgLatency ms`

---

## Project Structure

```
backend/
├── src/main/java/com/ims/backend/
│   ├── config/            # Kafka, Redis, R2DBC, MongoDB, WebFlux, Jackson
│   ├── controller/        # SignalController, IncidentController, RcaController, DashboardController
│   ├── service/           # SignalIngestionService, IncidentService, RcaService, DashboardService
│   ├── consumer/          # SignalConsumerService (Kafka listener)
│   ├── repository/
│   │   ├── relational/    # IncidentRepository, RcaRepository (R2DBC)
│   │   └── document/      # SignalRepository (MongoDB reactive)
│   ├── model/             # Incident, RcaRecord, Signal, IncidentStatus, Severity
│   ├── dto/               # SignalRequest, SignalEvent, RcaRequest, IncidentResponse, StatusTransitionRequest
│   ├── state/             # IncidentState, OpenState, InvestigatingState, ResolvedState, ClosedState
│   ├── strategy/          # AlertStrategy, RdbmsFailureStrategy, CacheFailureStrategy, NetworkFailureStrategy
│   ├── exception/         # GlobalExceptionHandler + custom exceptions
│   └── util/              # RateLimiter, ThroughputTracker, MetricsRegistry, IncidentEscalationScheduler
├── src/main/resources/
│   ├── application.yml
│   └── db/migration/V1__init_schema.sql
├── src/test/java/com/ims/backend/
│   ├── RcaValidationTest.java          # validate() unit tests
│   ├── RcaServiceReactiveTest.java     # StepVerifier reactive tests
│   ├── IncidentStateTest.java          # State machine tests
│   ├── AlertStrategyTest.java          # Strategy routing tests
│   └── SignalProcessingTest.java       # Parameterized severity + MTTR tests
└── Dockerfile

docker-compose.yml
sample-requests.sh
```

---

## Adding a New Alert Strategy

1. Create a class implementing `AlertStrategy`:

```java
@Component
public class DiskFullStrategy implements AlertStrategy {
    @Override
    public boolean supports(String type) {
        return "DISK_FULL".equals(type);
    }

    @Override
    public Severity determineSeverity(String type, String componentId) {
        return Severity.P1;
    }
}
```

2. Spring auto-injects it into `AlertStrategyResolver`'s `List<AlertStrategy>`. No other changes needed.

---

## Configuration Reference

Key `application.yml` knobs:

| Property | Default | Description |
|---|---|---|
| `ims.rate-limit.signals-per-second` | `10000` | Token refill rate |
| `ims.rate-limit.burst-capacity` | `20000` | Max burst tokens |
| `ims.debounce.window-seconds` | `10` | Dedup window per componentId |
| `ims.cache.incident-ttl-minutes` | `60` | Redis TTL for incident cache |
| `ims.kafka.topics.signals` | `ims.signals` | Kafka signal topic |
| `resilience4j.circuitbreaker.instances.mysqlDB.*` | see yml | CB config |

---

## Resilience Design

```
Request → Rate Limiter → Kafka (non-blocking)
                              ↓
                      Consumer (12 threads)
                              ↓
                    MongoDB save (CB + Retry)
                              ↓
                    RDBMS upsert (CB + Retry)
                              ↓
                    Redis cache write (CB – graceful degradation)
```

- **Kafka buffer**: absorbs traffic bursts; consumers process at their own pace  
- **Circuit Breakers**: open after 50% failure rate in a 10-request window (`mysqlDB` instance)  
- **Manual ACK**: Kafka offsets only committed on successful processing  
- **Fallback**: Redis-only read on MySQL circuit open  

---
