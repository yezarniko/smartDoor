package com.smartdoor.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class MqttDoorService implements MqttCallbackExtended {
    private static final Logger log = LoggerFactory.getLogger(MqttDoorService.class);
    private final ObjectMapper objectMapper;
    private final DeviceMessageService deviceMessages;

    @Value("${smartdoor.mqtt.host}") private String host;
    @Value("${smartdoor.mqtt.port}") private int port;
    @Value("${smartdoor.mqtt.username}") private String username;
    @Value("${smartdoor.mqtt.password}") private String password;
    private MqttAsyncClient client;

    public MqttDoorService(ObjectMapper objectMapper, DeviceMessageService deviceMessages) {
        this.objectMapper = objectMapper;
        this.deviceMessages = deviceMessages;
    }

    @PostConstruct
    void initialize() throws MqttException {
        client = new MqttAsyncClient("tcp://" + host + ":" + port,
                "smartdoor-backend-" + UUID.randomUUID(), new MemoryPersistence());
        client.setCallback(this);
        connect();
    }

    @Scheduled(fixedDelay = 5000)
    void reconnect() {
        if (client != null && !client.isConnected()) connect();
    }

    private synchronized void connect() {
        if (client == null || client.isConnected()) return;
        try {
            MqttConnectOptions options = new MqttConnectOptions();
            options.setUserName(username);
            options.setPassword(password.toCharArray());
            options.setAutomaticReconnect(true);
            options.setCleanSession(true);
            options.setConnectionTimeout(5);
            client.connect(options).waitForCompletion(6000);
        } catch (Exception exception) {
            log.warn("MQTT connection unavailable: {}", exception.getMessage());
        }
    }

    public boolean publishUnlock(String doorId, String requestId, long durationMs) {
        if (client == null || !client.isConnected()) return false;
        try {
            byte[] payload = objectMapper.writeValueAsBytes(Map.of(
                    "requestId", requestId,
                    "doorId", doorId,
                    "command", "UNLOCK",
                    "durationMs", durationMs,
                    "issuedAt", Instant.now().toString()));
            client.publish("smartdoor/" + doorId.toLowerCase(Locale.ROOT) + "/command",
                    payload, 1, false).waitForCompletion(3000);
            return true;
        } catch (Exception exception) {
            log.warn("MQTT publish failed: {}", exception.getMessage());
            return false;
        }
    }

    @Override
    public void connectComplete(boolean reconnect, String serverURI) {
        try { client.subscribe("smartdoor/+/+", 1); }
        catch (MqttException exception) { log.warn("MQTT subscription failed", exception); }
    }

    @Override
    public void connectionLost(Throwable cause) { log.warn("MQTT connection lost: {}", cause.getMessage()); }

    @Override
    public void messageArrived(String topic, MqttMessage message) {
        if (!topic.endsWith("/command")) deviceMessages.handle(topic, message.getPayload());
    }

    @Override public void deliveryComplete(IMqttDeliveryToken token) {}

    @PreDestroy
    void shutdown() throws MqttException {
        if (client != null && client.isConnected()) client.disconnect();
    }
}
