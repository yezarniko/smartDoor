# Local operations

## Start

1. Copy `.env.example` to `.env`.
2. Replace all passwords and set a random `SMARTDOOR_QR_SECRET` of at least 32 characters.
3. Run `docker compose up --build`.
4. Wait until `docker compose ps` reports MariaDB, Mosquitto, and backend healthy.
5. Open `http://localhost:5173` and sign in.

Create a user, open the user record, assign Main Door, save at least one schedule, then generate a QR. Open Webcam Scanner to scan it or upload the downloaded PNG.

## Select the door simulator

Docker Compose starts the Node/TypeScript virtual door (`SIM-DOOR-01`) by default. It is the fastest option for testing the complete software workflow without Wokwi:

```bash
docker compose up -d device-simulator
docker compose logs -f device-simulator
```

To replace it with the Wokwi ESP32 (`ESP32-DOOR-01`), stop only the Node simulator while leaving MariaDB, Mosquitto, the backend, and the frontend running:

```bash
docker compose stop device-simulator
```

Build and start the Wokwi project from the `firmware` directory. When finished with Wokwi, restore the Node simulator with:

```bash
docker compose start device-simulator
```

Do not normally run both actuator simulators together: both subscribe to `smartdoor/door-01/command`, so both will execute the same unlock request. See [device-simulation.md](device-simulation.md) for the complete setup, credential, gateway, verification, and troubleshooting instructions.

## Troubleshooting

- Webcam access works on `localhost`; LAN or internet hosting requires HTTPS.
- A `DEVICE_OFFLINE` decision means the simulator/ESP32 heartbeat is older than 20 seconds.
- If both virtual doors unlock, stop the simulator you are not currently testing.
- Wokwi must use its private/local IoT gateway to reach the Mosquitto broker on this computer; its public gateway cannot access `localhost`.
- Clear a development database with `docker compose down -v`; this permanently removes local SmartDoor data.
- Never use the development credentials or broker without TLS outside a trusted local network.

## Verification

Run backend tests with `cd backend && ./mvnw test`, frontend tests with `cd frontend && npm test`, simulator tests with `cd device-simulator && npm test`, and the browser smoke test against a running Compose stack with `cd frontend && npm run test:e2e`.
