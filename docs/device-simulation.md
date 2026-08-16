# Device simulation

SmartDoor supports two interchangeable door actuators. The Node/TypeScript simulator is the default software-only actuator. The Wokwi project runs the ESP32 firmware with a servo, red and green LEDs, and a buzzer. Both use the same MQTT contract, so the backend, webcam scanner, QR credentials, and administrator UI do not change.

The computer webcam remains the QR scanner in both modes. The simulated device receives only an authorized unlock command; raw QR tokens and user information are never sent to MQTT.

## Shared protocol

- Door identifier in payloads: `DOOR-01`
- Command topic: `smartdoor/door-01/command`
- Status topic: `smartdoor/door-01/status`
- Event topic: `smartdoor/door-01/event`
- Heartbeat topic: `smartdoor/door-01/heartbeat`
- Node device identifier: `SIM-DOOR-01`
- Wokwi/physical ESP32 identifier: `ESP32-DOOR-01`

Both devices validate the door ID, request ID, command age, unlock duration, and duplicate requests. Their normal state sequence is:

```text
LOCKED -> UNLOCKING -> UNLOCKED -> RELOCKING -> LOCKED
```

The default unlock duration is five seconds.

## Mode 1: Node virtual-device simulator

The Node simulator starts with the rest of the system:

```bash
cd /Users/yezarniko/Desktop/Projects/PharmacyASR/SmartDoor
docker compose up --build -d
```

Follow its state transitions with:

```bash
docker compose logs -f device-simulator
```

Open `http://localhost:5173`, sign in, and select **Door Status**. `SIM-DOOR-01` should report `LOCKED` and online. Create a user, grant Main Door permission, configure an allowed schedule, issue a QR credential, and scan it from **Webcam Scanner**. The log should show `UNLOCKING`, `UNLOCKED`, `RELOCKING`, and `LOCKED` for the same request ID.

Stop or restart only this simulator with:

```bash
docker compose stop device-simulator
docker compose start device-simulator
```

## Mode 2: Wokwi ESP32 simulator

### 1. Start the application without the Node actuator

Start the complete stack, then stop the Node simulator so only Wokwi responds to unlock commands:

```bash
cd /Users/yezarniko/Desktop/Projects/PharmacyASR/SmartDoor
docker compose up -d
docker compose stop device-simulator
```

MariaDB, Mosquitto, the backend, and the frontend must remain running. Docker publishes Mosquitto on TCP port `1883` of the computer.

### 2. Confirm the firmware credentials

The development defaults are defined in `firmware/include/secrets.example.h`:

```cpp
#define WIFI_SSID "Wokwi-GUEST"
#define WIFI_PASSWORD ""
#define MQTT_HOST "host.wokwi.internal"
#define MQTT_PORT 1883
#define MQTT_USERNAME "smartdoor"
#define MQTT_PASSWORD "smartdoor_mqtt_password"
```

`host.wokwi.internal` is Wokwi's hostname for the computer running the local broker. If `.env` contains different MQTT credentials, copy `firmware/include/secrets.example.h` to `firmware/include/secrets.h` and update the username and password. `secrets.h` is excluded from Git.

### 3. Build the ESP32 firmware

Open this directory as the project in VS Code:

```text
/Users/yezarniko/Desktop/Projects/PharmacyASR/SmartDoor/firmware
```

Install the **PlatformIO IDE** and **Wokwi Simulator** VS Code extensions. Build with PlatformIO:

```bash
pio run
```

The existing `wokwi.toml` loads:

```text
.pio/build/esp32dev/firmware.bin
.pio/build/esp32dev/firmware.elf
```

The checked-in `diagram.json` connects the ESP32 to the servo, LEDs, and buzzer.

### 4. Start Wokwi with local-network access

In VS Code, press `F1` and run **Wokwi: Request a New License** if the extension has not yet been activated. Build the firmware, then press `F1` and run **Wokwi: Start Simulator**.

Wokwi for VS Code includes a private IoT gateway that allows the simulated ESP32 to connect to services on the computer. The browser-based Wokwi public gateway cannot reach the local Mosquitto broker. A browser simulation therefore requires Wokwi's separately enabled Private IoT Gateway; Chrome, Firefox, or Edge should be used because the private gateway is not supported in Safari.

Official references:

- [Wokwi ESP32 Wi-Fi and private gateway](https://docs.wokwi.com/guides/esp32-wifi)
- [Wokwi for VS Code setup](https://docs.wokwi.com/vscode/getting-started)
- [Wokwi project and IoT gateway configuration](https://docs.wokwi.com/vscode/project-config)

### 5. Verify the Wokwi connection

1. Open `http://localhost:5173` and sign in.
2. Open **Door Status**.
3. Wait for `ESP32-DOOR-01` to show `LOCKED` and online. A heartbeat is published every five seconds.
4. Create or select a user with Main Door permission and a currently allowed schedule.
5. Issue a QR credential and scan it using the computer webcam or upload its PNG.
6. Confirm that the Wokwi servo unlocks, the green LED activates, the buzzer sounds, and the door relocks after five seconds.
7. Open **Access Logs** and confirm that the request progresses from `GRANTED_COMMAND_SENT` to `UNLOCKED`.

### 6. Return to the Node simulator

Stop the Wokwi simulation before restoring the Node actuator:

```bash
cd /Users/yezarniko/Desktop/Projects/PharmacyASR/SmartDoor
docker compose start device-simulator
```

## Troubleshooting

### `ESP32-DOOR-01` remains offline

- Confirm the Wokwi private/local IoT gateway is active.
- Confirm Mosquitto is running and healthy with `docker compose ps`.
- Confirm port `1883` is not blocked by a local firewall.
- Confirm `MQTT_HOST` is `host.wokwi.internal`, not `localhost` or the Docker service name `mosquitto`.
- Confirm the firmware MQTT username and password match `.env`.
- Wait at least five seconds for the next heartbeat, then refresh **Door Status**.

### MQTT authentication fails

Rebuild the firmware after changing `secrets.h`. The broker credentials are embedded at compile time. Restarting Wokwi without rebuilding continues to use the old values.

### Access is granted but the servo does not move

- Check **Access Logs** for `DEVICE_OFFLINE`, `DEVICE_TIMEOUT`, or `DEVICE_ERROR`.
- Confirm the request ID appears in Wokwi's serial/simulation output.
- Confirm `diagram.json` connects the servo PWM pin to ESP32 GPIO 18.
- Confirm the ESP32 clock synchronized through NTP; commands older than ten seconds are rejected.

### Both simulated doors unlock

The Node simulator and Wokwi are both subscribed to the same door command topic. Stop the Node simulator when testing Wokwi, or stop Wokwi when testing the Node simulator.
