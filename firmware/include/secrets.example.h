#pragma once
#define WIFI_SSID "Wokwi-GUEST"
#define WIFI_PASSWORD ""
#define MQTT_HOST "host.wokwi.internal"
#define MQTT_PORT 1883
#define MQTT_USERNAME "smartdoor"
#define MQTT_PASSWORD "smartdoor_mqtt_password"

// Keep 0 for the Wokwi servo. Set to 1 in secrets.h for the physical
// high-active relay and solenoid circuit documented in physical-prototype.md.
#define SMARTDOOR_RELAY_MODE 0
