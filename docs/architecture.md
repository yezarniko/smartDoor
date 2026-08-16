# Architecture

## Trust boundaries

The computer webcam decodes the QR credential in the React terminal. The opaque token is submitted directly to Spring Boot. Spring Boot validates the signature and database state, evaluates policy and the J48 classifier, records the decision, then publishes only an unlock command to MQTT. Raw QR tokens and personal data never reach MQTT or ESP32.

```text
Admin/Scanner UI -> Spring Boot -> MariaDB
                         |
                         +-> Mosquitto -> simulator / Wokwi / ESP32
                         +-> SSE -> Admin UI
```

## MQTT contract

- Command: `smartdoor/{doorId}/command`
- Status: `smartdoor/{doorId}/status`
- Event: `smartdoor/{doorId}/event`
- Heartbeat: `smartdoor/{doorId}/heartbeat`

Commands are QoS 1, non-retained, door-scoped, valid for ten seconds, and idempotent by `requestId`. Status is retained. Devices start locked, automatically relock, and publish a heartbeat every five seconds.

## Access precedence

Signature, credential state, expiry, usage limit, user status, terminal registration, door permission, schedule, model result, and actuator availability must all pass. The J48 classifier cannot override a failed mandatory control.

