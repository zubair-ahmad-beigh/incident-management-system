###############################################################################
#  Incident Management System – Sample API Requests (HTTPie / curl)
###############################################################################

BASE=http://localhost:8080

# ─────────────────────────────────────────────────────────────────────────────
# 1. SIGNAL INGESTION
# ─────────────────────────────────────────────────────────────────────────────

## POST /api/signals – DB failure (P0)
curl -s -X POST "$BASE/api/signals" \
  -H "Content-Type: application/json" \
  -d '{
    "componentId": "order-service",
    "type":        "DB_FAILURE",
    "message":     "Connection pool exhausted – max 50 connections reached",
    "timestamp":   "2024-06-15T10:30:00Z"
  }' | jq .

## POST /api/signals – Cache miss (P2)
curl -s -X POST "$BASE/api/signals" \
  -H "Content-Type: application/json" \
  -d '{
    "componentId": "product-service",
    "type":        "CACHE_MISS",
    "message":     "Redis TTL expired on product catalog key",
    "timestamp":   "2024-06-15T10:31:00Z"
  }' | jq .

## POST /api/signals – Network failure (P1)
curl -s -X POST "$BASE/api/signals" \
  -H "Content-Type: application/json" \
  -d '{
    "componentId": "payment-service",
    "type":        "NETWORK_ERROR",
    "message":     "Gateway timeout reaching stripe.com – 3 retries exhausted",
    "timestamp":   "2024-06-15T10:32:00Z"
  }' | jq .


# ─────────────────────────────────────────────────────────────────────────────
# 2. INCIDENT LIFECYCLE
# ─────────────────────────────────────────────────────────────────────────────

## List all OPEN incidents
curl -s "$BASE/api/incidents?status=OPEN" | jq .

## Get specific incident by ID (replace with real UUID)
INCIDENT_ID="your-incident-uuid-here"
curl -s "$BASE/api/incidents/$INCIDENT_ID" | jq .

## Get incidents by component
curl -s "$BASE/api/incidents?component=order-service" | jq .

## Transition: OPEN → INVESTIGATING
curl -s -X PATCH "$BASE/api/incidents/$INCIDENT_ID/status" \
  -H "Content-Type: application/json" \
  -d '{"targetStatus": "INVESTIGATING"}' | jq .

## Transition: INVESTIGATING → RESOLVED  (computes MTTR automatically)
curl -s -X PATCH "$BASE/api/incidents/$INCIDENT_ID/status" \
  -H "Content-Type: application/json" \
  -d '{"targetStatus": "RESOLVED"}' | jq .

## Transition: RESOLVED → CLOSED  (will fail without RCA – expected HTTP 409)
curl -s -X PATCH "$BASE/api/incidents/$INCIDENT_ID/status" \
  -H "Content-Type: application/json" \
  -d '{"targetStatus": "CLOSED"}' | jq .


# ─────────────────────────────────────────────────────────────────────────────
# 3. RCA MANAGEMENT
# ─────────────────────────────────────────────────────────────────────────────

## Submit a complete RCA (required before CLOSED transition)
curl -s -X POST "$BASE/api/rca" \
  -H "Content-Type: application/json" \
  -d "{
    \"incidentId\":       \"$INCIDENT_ID\",
    \"rootCause\":        \"Database connection pool exhausted due to N+1 query bug in OrderRepository.findAllWithItems()\",
    \"fixApplied\":       \"Rewrote query to use JOIN FETCH, increased pool size from 10 to 50, added connection timeout\",
    \"preventionSteps\":  \"Added query plan analysis step to CI; set Datadog alert for pool saturation > 80%; scheduled quarterly DB review\"
  }" | jq .

## Get RCA for an incident
curl -s "$BASE/api/rca/$INCIDENT_ID" | jq .

## Now transition to CLOSED (succeeds after RCA submitted)
curl -s -X PATCH "$BASE/api/incidents/$INCIDENT_ID/status" \
  -H "Content-Type: application/json" \
  -d '{"targetStatus": "CLOSED"}' | jq .


# ─────────────────────────────────────────────────────────────────────────────
# 4. DASHBOARD & OBSERVABILITY
# ─────────────────────────────────────────────────────────────────────────────

## Real-time dashboard summary
curl -s "$BASE/api/dashboard/summary" | jq .

## Health check
curl -s "$BASE/actuator/health" | jq .

## Prometheus metrics
curl -s "$BASE/actuator/prometheus" | grep ims_


# ─────────────────────────────────────────────────────────────────────────────
# 5. ERROR CASES (expected failures)
# ─────────────────────────────────────────────────────────────────────────────

## Missing required field → HTTP 400 Bad Request
curl -s -X POST "$BASE/api/signals" \
  -H "Content-Type: application/json" \
  -d '{"componentId": "svc", "type": "DB_FAILURE"}' | jq .

## Incomplete RCA → HTTP 422 Unprocessable Entity
curl -s -X POST "$BASE/api/rca" \
  -H "Content-Type: application/json" \
  -d "{
    \"incidentId\":   \"$INCIDENT_ID\",
    \"rootCause\":    \"\",
    \"fixApplied\":   \"fix\",
    \"preventionSteps\": \"steps\"
  }" | jq .

## Non-existent incident → HTTP 404
curl -s "$BASE/api/incidents/00000000-0000-0000-0000-000000000000" | jq .

## Invalid state transition (OPEN → CLOSED) → HTTP 409
curl -s -X PATCH "$BASE/api/incidents/$INCIDENT_ID/status" \
  -H "Content-Type: application/json" \
  -d '{"targetStatus": "CLOSED"}' | jq .
