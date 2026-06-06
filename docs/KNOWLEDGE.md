# swim-dnotam-consumer-validator — Knowledge Base

## What This Is

**Mock AISP (EAD simulator) for testing the DNOTAM Consumer.** Provides everything a real EUROCONTROL/EAD provider would expose, allowing the consumer to be developed and validated without access to production systems.

This is what `swim-digital-notam-consumer` connects to during development. Never the other way around.

## What It Provides

| Component | Purpose |
|-----------|---------|
| **Subscription Manager REST API** | Full lifecycle: create, get, update (ACTIVE/PAUSED), delete subscriptions; list topics; WFS features |
| **Artemis AMQP Broker** | Receives consumer connections, delivers DNOTAM events and heartbeats |
| **Event Generator** | Publishes 82 real AIXM 5.1.1 XML event samples covering all 7 CP1 scenarios |
| **Heartbeat Publisher** | Per-subscription heartbeat to `{queue}.heartbeat` (15s interval) |

## Consumer Connection Config

```yaml
providers:
  - providerId: "eurocontrol-ead"
    subscriptionManager:
      url: "https://dnotam-consumer-validator-<namespace>.apps.<cluster>"
    amqpBroker:
      host: "dnotam-consumer-validator-artemis-<namespace>.apps.<cluster>"
```

## Supported Event Scenarios

RWY.CLS, AD.CLS, RWY.LIM, SFC.CON, SAA.ACT, OBS.NEW, NAV.UNS (all 7 CP1 DNOTAM scenarios).

## Build & Run

```bash
./mvnw clean package -DskipTests

# Dev mode
quarkus dev
```

Local stack: `podman compose up -d` (requires compose.yml with Kafka, MongoDB, Artemis).

OpenShift: deploy via Helm or Operator CR. Validator exposes its own Artemis.
