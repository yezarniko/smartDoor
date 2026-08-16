# Physical prototype

The production scan terminal remains a computer with a built-in or USB webcam. It runs the browser Scanner page. The ESP32 receives only verified actuator commands over MQTT.

## Components

- ESP32 DevKit
- Opto-isolated relay module rated for the chosen lock
- 12 V fail-secure solenoid lock and correctly sized 12 V supply
- Flyback protection appropriate to the lock/relay arrangement
- Red and green LEDs with 220–330 Ω current-limiting resistors, buzzer, optional magnetic door sensor

## Safe integration sequence

1. Test firmware with LEDs and servo in Wokwi.
2. Test the physical ESP32, LEDs, and MQTT with no lock connected.
3. Test relay switching with a meter or harmless low-voltage load.
4. Have the final lock circuit reviewed by a qualified person before energizing it.
5. Power the lock separately from ESP32 logic; never drive a solenoid from a GPIO pin.
6. Confirm locked startup, five-second relock, network-loss behavior, and manual release.

Do not install this academic prototype on an emergency exit or rely on it as the sole life-safety access mechanism.
