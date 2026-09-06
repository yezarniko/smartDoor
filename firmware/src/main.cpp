#include <Arduino.h>
#include <WiFi.h>
#include <PubSubClient.h>
#include <ArduinoJson.h>
#include <time.h>

#if __has_include("secrets.h")
#include "secrets.h"
#else
#include "secrets.example.h"
#endif

#ifndef SMARTDOOR_RELAY_MODE
#define SMARTDOOR_RELAY_MODE 0
#endif

#if !SMARTDOOR_RELAY_MODE
#include <ESP32Servo.h>
#endif

constexpr char DOOR_ID[] = "DOOR-01";
constexpr char DEVICE_ID[] = "ESP32-DOOR-01";
constexpr int ACTUATOR_PIN = 18;
constexpr int RED_LED_PIN = 19;
constexpr int GREEN_LED_PIN = 21;
constexpr int BUZZER_PIN = 5;
constexpr unsigned long WIFI_CONNECT_TIMEOUT_MS = 30000;
constexpr unsigned long WIFI_RETRY_INTERVAL_MS = 5000;
constexpr unsigned long MQTT_RETRY_INTERVAL_MS = 3000;
constexpr unsigned long HEARTBEAT_INTERVAL_MS = 5000;
constexpr unsigned long DIAGNOSTIC_INTERVAL_MS = 10000;
#if !SMARTDOOR_RELAY_MODE
constexpr int LOCKED_ANGLE = 0;
constexpr int UNLOCKED_ANGLE = 90;
#endif

WiFiClient wifiClient;
PubSubClient mqtt(wifiClient);
#if !SMARTDOOR_RELAY_MODE
Servo lockServo;
#endif

String state = "LOCKED";
String lastRequestId;
unsigned long relockAt = 0;
unsigned long lastHeartbeat = 0;
unsigned long lastWifiAttempt = 0;
unsigned long lastMqttAttempt = 0;
unsigned long lastDiagnostic = 0;
unsigned long heartbeatSequence = 0;

const char* wifiStatusName(wl_status_t status) {
  switch (status) {
    case WL_IDLE_STATUS: return "IDLE";
    case WL_NO_SSID_AVAIL: return "SSID_NOT_FOUND";
    case WL_SCAN_COMPLETED: return "SCAN_COMPLETED";
    case WL_CONNECTED: return "CONNECTED";
    case WL_CONNECT_FAILED: return "CONNECT_FAILED";
    case WL_CONNECTION_LOST: return "CONNECTION_LOST";
    case WL_DISCONNECTED: return "DISCONNECTED";
    default: return "UNKNOWN";
  }
}

const char* mqttStateName(int stateCode) {
  switch (stateCode) {
    case MQTT_CONNECTION_TIMEOUT: return "CONNECTION_TIMEOUT";
    case MQTT_CONNECTION_LOST: return "CONNECTION_LOST";
    case MQTT_CONNECT_FAILED: return "TCP_CONNECT_FAILED";
    case MQTT_DISCONNECTED: return "DISCONNECTED";
    case MQTT_CONNECTED: return "CONNECTED";
    case MQTT_CONNECT_BAD_PROTOCOL: return "BAD_PROTOCOL";
    case MQTT_CONNECT_BAD_CLIENT_ID: return "BAD_CLIENT_ID";
    case MQTT_CONNECT_UNAVAILABLE: return "BROKER_UNAVAILABLE";
    case MQTT_CONNECT_BAD_CREDENTIALS: return "BAD_CREDENTIALS";
    case MQTT_CONNECT_UNAUTHORIZED: return "UNAUTHORIZED";
    default: return "UNKNOWN";
  }
}

String topic(const char* leaf) {
  String doorTopic = DOOR_ID;
  doorTopic.toLowerCase();
  return "smartdoor/" + doorTopic + "/" + leaf;
}

void lockActuator() {
#if SMARTDOOR_RELAY_MODE
  digitalWrite(ACTUATOR_PIN, LOW);
#else
  lockServo.write(LOCKED_ANGLE);
#endif
}

void unlockActuator() {
#if SMARTDOOR_RELAY_MODE
  digitalWrite(ACTUATOR_PIN, HIGH);
#else
  lockServo.write(UNLOCKED_ANGLE);
#endif
}

void forceLockedOutput() {
  lockActuator();
  relockAt = 0;
  state = "LOCKED";
  digitalWrite(RED_LED_PIN, HIGH);
  digitalWrite(GREEN_LED_PIN, LOW);
}

void printNetworkInfo() {
  Serial.println("[WIFI] Connected");
  Serial.printf("[WIFI] SSID: %s\n", WiFi.SSID().c_str());
  Serial.printf("[WIFI] IP: %s\n", WiFi.localIP().toString().c_str());
  Serial.printf("[WIFI] Gateway: %s\n", WiFi.gatewayIP().toString().c_str());
  Serial.printf("[WIFI] DNS: %s\n", WiFi.dnsIP().toString().c_str());
  Serial.printf("[WIFI] MAC: %s\n", WiFi.macAddress().c_str());
  Serial.printf("[WIFI] RSSI: %d dBm\n", WiFi.RSSI());
}

void printHealth() {
  const int mqttCode = mqtt.state();
  Serial.printf(
      "[HEALTH] uptime=%lus wifi=%s ip=%s rssi=%d mqtt=%s(%d) door=%s heartbeatSeq=%lu\n",
      millis() / 1000,
      wifiStatusName(WiFi.status()),
      WiFi.localIP().toString().c_str(),
      WiFi.status() == WL_CONNECTED ? WiFi.RSSI() : 0,
      mqttStateName(mqttCode),
      mqttCode,
      state.c_str(),
      heartbeatSequence);
}

bool publishState(const char* leaf, const String& requestId = "") {
  JsonDocument document;
  document["deviceId"] = DEVICE_ID;
  document["doorId"] = DOOR_ID;
  document["state"] = state;
  if (requestId.length()) document["requestId"] = requestId;
  document["at"] = static_cast<long long>(time(nullptr));
  document["uptimeMs"] = millis();
  document["ip"] = WiFi.localIP().toString();
  document["rssi"] = WiFi.RSSI();
  if (strcmp(leaf, "heartbeat") == 0) document["sequence"] = ++heartbeatSequence;

  String payload;
  serializeJson(document, payload);
  const String publishTopic = topic(leaf);
  const bool retained = strcmp(leaf, "status") == 0;
  const bool published = mqtt.publish(publishTopic.c_str(), payload.c_str(), retained);
  Serial.printf("[MQTT] PUB %s retained=%s result=%s payload=%s\n",
                publishTopic.c_str(), retained ? "yes" : "no",
                published ? "OK" : "FAILED", payload.c_str());
  return published;
}

void setState(const String& next, const String& requestId = "") {
  Serial.printf("[STATE] %s -> %s requestId=%s\n",
                state.c_str(), next.c_str(), requestId.length() ? requestId.c_str() : "-");
  state = next;
  digitalWrite(RED_LED_PIN, state == "LOCKED" ? HIGH : LOW);
  digitalWrite(GREEN_LED_PIN, state == "UNLOCKED" ? HIGH : LOW);
  publishState("status", requestId);
  publishState("event", requestId);
}

time_t parseIsoUtc(const char* value) {
  struct tm timeinfo = {};
  if (!strptime(value, "%Y-%m-%dT%H:%M:%S", &timeinfo)) return 0;
  timeinfo.tm_isdst = 0;
  return mktime(&timeinfo);
}

void rejectCommand(const char* reason) {
  Serial.printf("[COMMAND] Rejected: %s\n", reason);
}

void handleCommand(char* incomingTopic, byte* payload, unsigned int length) {
  String rawPayload;
  rawPayload.reserve(length);
  for (unsigned int index = 0; index < length; ++index) rawPayload += static_cast<char>(payload[index]);
  Serial.printf("[MQTT] RX %s bytes=%u payload=%s\n", incomingTopic, length, rawPayload.c_str());

  JsonDocument document;
  const DeserializationError jsonError = deserializeJson(document, payload, length);
  if (jsonError) {
    Serial.printf("[COMMAND] Invalid JSON: %s\n", jsonError.c_str());
    return;
  }

  String requestId = document["requestId"] | "";
  String doorId = document["doorId"] | "";
  String command = document["command"] | "";
  unsigned long durationMs = document["durationMs"] | 0;
  const char* issuedAt = document["issuedAt"] | "";
  const time_t issued = parseIsoUtc(issuedAt);
  const time_t now = time(nullptr);

  if (!requestId.length()) return rejectCommand("missing requestId");
  if (requestId == lastRequestId) return rejectCommand("duplicate requestId");
  if (doorId != DOOR_ID) return rejectCommand("doorId does not match DOOR-01");
  if (command != "UNLOCK") return rejectCommand("unsupported command");
  if (durationMs < 1000 || durationMs > 30000) return rejectCommand("durationMs must be 1000-30000");
  if (issued == 0) return rejectCommand("issuedAt is missing or invalid");
  if (now < 1700000000) return rejectCommand("ESP32 clock is not synchronized");

  const long clockSkewSeconds = labs(static_cast<long>(now - issued));
  if (clockSkewSeconds > 10) {
    Serial.printf("[COMMAND] Rejected: command clock skew is %ld seconds (maximum 10)\n", clockSkewSeconds);
    return;
  }

  Serial.printf("[COMMAND] Accepted requestId=%s duration=%lums\n", requestId.c_str(), durationMs);
  lastRequestId = requestId;
  setState("UNLOCKING", requestId);
  unlockActuator();
  tone(BUZZER_PIN, 1600, 120);
  delay(350);
  setState("UNLOCKED", requestId);
  relockAt = millis() + durationMs;
}

bool syncClock() {
  Serial.print("[TIME] Synchronizing with pool.ntp.org");
  configTime(0, 0, "pool.ntp.org");
  const unsigned long startedAt = millis();
  while (time(nullptr) < 1700000000 && millis() - startedAt < 15000) {
    delay(500);
    Serial.print('.');
  }
  Serial.println();

  const time_t now = time(nullptr);
  if (now < 1700000000) {
    Serial.println("[TIME] FAILED: unlock commands will be rejected until time synchronizes");
    return false;
  }

  struct tm utc = {};
  gmtime_r(&now, &utc);
  char formatted[30];
  strftime(formatted, sizeof(formatted), "%Y-%m-%dT%H:%M:%SZ", &utc);
  Serial.printf("[TIME] Synchronized: %s\n", formatted);
  return true;
}

bool connectWifi() {
  if (WiFi.status() == WL_CONNECTED) return true;
  lastWifiAttempt = millis();

  Serial.println();
  Serial.printf("[WIFI] Connecting to SSID: %s\n", WIFI_SSID);
  Serial.println("[WIFI] ESP32 supports 2.4 GHz Wi-Fi; the password is intentionally not printed");
  WiFi.mode(WIFI_STA);
  WiFi.persistent(false);
  WiFi.setAutoReconnect(true);
  WiFi.setSleep(false);
  WiFi.begin(WIFI_SSID, WIFI_PASSWORD);

  const unsigned long startedAt = millis();
  unsigned long lastProgress = 0;
  while (WiFi.status() != WL_CONNECTED && millis() - startedAt < WIFI_CONNECT_TIMEOUT_MS) {
    if (millis() - lastProgress >= 1000) {
      lastProgress = millis();
      Serial.printf("[WIFI] Waiting... status=%s(%d) elapsed=%lus\n",
                    wifiStatusName(WiFi.status()), WiFi.status(),
                    (millis() - startedAt) / 1000);
    }
    delay(100);
  }

  if (WiFi.status() != WL_CONNECTED) {
    Serial.printf("[WIFI] FAILED after %lus: %s(%d)\n",
                  WIFI_CONNECT_TIMEOUT_MS / 1000,
                  wifiStatusName(WiFi.status()), WiFi.status());
    Serial.println("[WIFI] Check exact SSID/password, 2.4 GHz availability, signal, and client isolation");
    return false;
  }

  printNetworkInfo();
  syncClock();
  return true;
}

bool connectMqtt() {
  if (mqtt.connected()) return true;
  if (WiFi.status() != WL_CONNECTED) {
    Serial.println("[MQTT] Skipped: Wi-Fi is not connected");
    return false;
  }
  lastMqttAttempt = millis();

  const String statusTopic = topic("status");
  const String commandTopic = topic("command");
  Serial.println();
  Serial.printf("[MQTT] Connecting to %s:%d as user=%s clientId=%s\n",
                MQTT_HOST, MQTT_PORT, MQTT_USERNAME, DEVICE_ID);
  Serial.println("[MQTT] MQTT_HOST must be the SmartDoor computer LAN IP, not localhost or mosquitto");

  const bool connected = mqtt.connect(
      DEVICE_ID,
      MQTT_USERNAME,
      MQTT_PASSWORD,
      statusTopic.c_str(),
      1,
      true,
      "{\"deviceId\":\"ESP32-DOOR-01\",\"doorId\":\"DOOR-01\",\"state\":\"OFFLINE\"}");

  if (!connected) {
    const int stateCode = mqtt.state();
    Serial.printf("[MQTT] FAILED: %s (%d)\n", mqttStateName(stateCode), stateCode);
    if (stateCode == MQTT_CONNECT_FAILED || stateCode == MQTT_CONNECTION_TIMEOUT) {
      Serial.println("[MQTT] Check MQTT_HOST, Windows firewall TCP 1883, Docker port 1883, and broker health");
    } else if (stateCode == MQTT_CONNECT_BAD_CREDENTIALS || stateCode == MQTT_CONNECT_UNAUTHORIZED) {
      Serial.println("[MQTT] MQTT username/password do not match the values used by Docker Compose");
    }
    return false;
  }

  Serial.println("[MQTT] Connected to broker");
  if (!mqtt.subscribe(commandTopic.c_str(), 1)) {
    Serial.printf("[MQTT] FAILED to subscribe: %s\n", commandTopic.c_str());
    mqtt.disconnect();
    return false;
  }
  Serial.printf("[MQTT] Subscribed: %s\n", commandTopic.c_str());

  forceLockedOutput();
  if (!publishState("status")) {
    Serial.println("[MQTT] Initial status publish failed; forcing reconnect");
    mqtt.disconnect();
    return false;
  }
  lastHeartbeat = 0;
  Serial.println("[MQTT] Initial LOCKED status sent; the UI should become online after the backend receives it");
  return true;
}

void setup() {
  Serial.begin(115200);
  delay(700);
  Serial.println();
  Serial.println("============================================================");
  Serial.println("SmartDoor ESP32 diagnostic firmware");
  Serial.printf("Device=%s Door=%s RelayMode=%d ActuatorGPIO=%d\n",
                DEVICE_ID, DOOR_ID, SMARTDOOR_RELAY_MODE, ACTUATOR_PIN);
  Serial.println("Serial speed: 115200 baud");
  Serial.println("============================================================");

  pinMode(RED_LED_PIN, OUTPUT);
  pinMode(GREEN_LED_PIN, OUTPUT);
  pinMode(BUZZER_PIN, OUTPUT);
#if SMARTDOOR_RELAY_MODE
  digitalWrite(ACTUATOR_PIN, LOW);
  pinMode(ACTUATOR_PIN, OUTPUT);
#else
  lockServo.attach(ACTUATOR_PIN);
#endif
  forceLockedOutput();
  Serial.println("[SAFETY] Actuator forced to LOCKED output");

  mqtt.setServer(MQTT_HOST, MQTT_PORT);
  mqtt.setCallback(handleCommand);
  mqtt.setBufferSize(512);
  mqtt.setKeepAlive(15);
  mqtt.setSocketTimeout(5);

  if (connectWifi()) connectMqtt();
  printHealth();
}

void loop() {
  static wl_status_t previousWifiStatus = WL_IDLE_STATUS;
  static bool previousMqttConnected = false;

  const wl_status_t currentWifiStatus = WiFi.status();
  if (currentWifiStatus != previousWifiStatus) {
    Serial.printf("[WIFI] State changed: %s(%d) -> %s(%d)\n",
                  wifiStatusName(previousWifiStatus), previousWifiStatus,
                  wifiStatusName(currentWifiStatus), currentWifiStatus);
    previousWifiStatus = currentWifiStatus;
  }

  if (currentWifiStatus != WL_CONNECTED) {
    if (mqtt.connected()) mqtt.disconnect();
    if (state != "LOCKED" || relockAt) {
      Serial.println("[SAFETY] Wi-Fi lost; forcing locked output");
      forceLockedOutput();
    }
    if (lastWifiAttempt == 0 || millis() - lastWifiAttempt >= WIFI_RETRY_INTERVAL_MS) connectWifi();
    delay(20);
    return;
  }

  const bool mqttConnected = mqtt.connected();
  if (mqttConnected != previousMqttConnected) {
    Serial.printf("[MQTT] State changed: %s\n", mqttConnected ? "CONNECTED" : "DISCONNECTED");
    previousMqttConnected = mqttConnected;
  }

  if (!mqttConnected) {
    if (state != "LOCKED" || relockAt) {
      Serial.println("[SAFETY] MQTT lost; forcing locked output");
      forceLockedOutput();
    }
    if (lastMqttAttempt == 0 || millis() - lastMqttAttempt >= MQTT_RETRY_INTERVAL_MS) connectMqtt();
    delay(20);
    return;
  }

  if (!mqtt.loop()) {
    Serial.printf("[MQTT] mqtt.loop failed: %s (%d)\n", mqttStateName(mqtt.state()), mqtt.state());
  }

  if (relockAt && static_cast<long>(millis() - relockAt) >= 0) {
    setState("RELOCKING", lastRequestId);
    lockActuator();
    delay(350);
    setState("LOCKED", lastRequestId);
    relockAt = 0;
  }

  if (millis() - lastHeartbeat >= HEARTBEAT_INTERVAL_MS) {
    if (!publishState("heartbeat")) {
      Serial.println("[MQTT] Heartbeat publish failed; disconnecting so the retry path can recover");
      mqtt.disconnect();
    }
    lastHeartbeat = millis();
  }

  if (millis() - lastDiagnostic >= DIAGNOSTIC_INTERVAL_MS) {
    printHealth();
    lastDiagnostic = millis();
  }

  delay(2);
}
