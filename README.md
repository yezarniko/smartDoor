# SmartDoor

Software-first smart door access control using administrator-issued QR credentials, a computer webcam, an explainable WEKA J48 classifier, MariaDB, MQTT, and an ESP32-compatible device protocol.

## Quick start

1. Copy `.env.example` to `.env` and replace the development secrets.
2. Run `docker compose up --build`.
3. Open `http://localhost:5173`.
4. Sign in with `SMARTDOOR_ADMIN_USERNAME` and `SMARTDOOR_ADMIN_PASSWORD`.

The first startup seeds `DOOR-01`, terminal `TERMINAL-01`, simulated actuator `SIM-DOOR-01`, and the configured administrator.

## Local endpoints

- UI: `http://localhost:5173`
- API: `http://localhost:8080`
- MariaDB: `localhost:3306`
- MQTT: `localhost:1883`

See [docs/architecture.md](docs/architecture.md) and [docs/operations.md](docs/operations.md) for the system contract and operating procedure. The complete Node simulator and Wokwi connection procedure is in [docs/device-simulation.md](docs/device-simulation.md). For a physical ESP32 build, use the [hardware requirements, jumper map, and setup guide](docs/physical-prototype.md).
