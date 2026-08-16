#include <Arduino.h>
#include <WiFi.h>
#include <PubSubClient.h>
#include <ArduinoJson.h>
#include <ESP32Servo.h>
#include <time.h>

#if __has_include("secrets.h")
#include "secrets.h"
#else
#include "secrets.example.h"
#endif

constexpr char DOOR_ID[] = "DOOR-01";
constexpr char DEVICE_ID[] = "ESP32-DOOR-01";
constexpr int SERVO_PIN = 18;
constexpr int RED_LED_PIN = 19;
constexpr int GREEN_LED_PIN = 21;
constexpr int BUZZER_PIN = 5;
constexpr int LOCKED_ANGLE = 0;
constexpr int UNLOCKED_ANGLE = 90;

WiFiClient wifiClient;
PubSubClient mqtt(wifiClient);
Servo lockServo;
String state = "LOCKED";
String lastRequestId;
unsigned long relockAt = 0;
unsigned long lastHeartbeat = 0;

String topic(const char* leaf) {
  String doorTopic = DOOR_ID;
  doorTopic.toLowerCase();
  return "smartdoor/" + doorTopic + "/" + leaf;
}

void publishState(const char* leaf, const String& requestId = "") {
  JsonDocument document;
  document["deviceId"] = DEVICE_ID;
  document["doorId"] = DOOR_ID;
  document["state"] = state;
  if (requestId.length()) document["requestId"] = requestId;
  document["at"] = static_cast<long long>(time(nullptr));
  String payload;
  serializeJson(document, payload);
  mqtt.publish(topic(leaf).c_str(), payload.c_str(), strcmp(leaf, "status") == 0);
}

void setState(const String& next, const String& requestId = "") {
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

void handleCommand(char*, byte* payload, unsigned int length) {
  JsonDocument document;
  if (deserializeJson(document, payload, length)) return;
  String requestId = document["requestId"] | "";
  String doorId = document["doorId"] | "";
  String command = document["command"] | "";
  unsigned long durationMs = document["durationMs"] | 0;
  const char* issuedAt = document["issuedAt"] | "";
  time_t issued = parseIsoUtc(issuedAt);
  time_t now = time(nullptr);

  if (!requestId.length() || requestId == lastRequestId || doorId != DOOR_ID || command != "UNLOCK") return;
  if (durationMs < 1000 || durationMs > 30000 || issued == 0 || abs(static_cast<long>(now - issued)) > 10) return;
  lastRequestId = requestId;
  setState("UNLOCKING", requestId);
  lockServo.write(UNLOCKED_ANGLE);
  tone(BUZZER_PIN, 1600, 120);
  delay(350);
  setState("UNLOCKED", requestId);
  relockAt = millis() + durationMs;
}

void connectWifi() {
  WiFi.begin(WIFI_SSID, WIFI_PASSWORD);
  while (WiFi.status() != WL_CONNECTED) delay(250);
  configTime(0, 0, "pool.ntp.org");
}

void connectMqtt() {
  while (!mqtt.connected()) {
    if (mqtt.connect(DEVICE_ID, MQTT_USERNAME, MQTT_PASSWORD,
                     topic("status").c_str(), 1, true,
                     "{\"deviceId\":\"ESP32-DOOR-01\",\"doorId\":\"DOOR-01\",\"state\":\"OFFLINE\"}")) {
      mqtt.subscribe(topic("command").c_str(), 1);
      setState("LOCKED");
    } else delay(2000);
  }
}

void setup() {
  Serial.begin(115200);
  pinMode(RED_LED_PIN, OUTPUT);
  pinMode(GREEN_LED_PIN, OUTPUT);
  pinMode(BUZZER_PIN, OUTPUT);
  lockServo.attach(SERVO_PIN);
  lockServo.write(LOCKED_ANGLE);
  digitalWrite(RED_LED_PIN, HIGH);
  connectWifi();
  mqtt.setServer(MQTT_HOST, MQTT_PORT);
  mqtt.setCallback(handleCommand);
  mqtt.setBufferSize(512);
}

void loop() {
  if (WiFi.status() != WL_CONNECTED) connectWifi();
  if (!mqtt.connected()) connectMqtt();
  mqtt.loop();
  if (relockAt && millis() >= relockAt) {
    setState("RELOCKING", lastRequestId);
    lockServo.write(LOCKED_ANGLE);
    delay(350);
    setState("LOCKED", lastRequestId);
    relockAt = 0;
  }
  if (millis() - lastHeartbeat >= 5000) {
    publishState("heartbeat");
    lastHeartbeat = millis();
  }
}
