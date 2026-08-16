package com.smartdoor.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartdoor.domain.Device;
import com.smartdoor.repository.AccessEventRepository;
import com.smartdoor.repository.DeviceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;

import static com.smartdoor.domain.Enums.DeviceStatus;
import static com.smartdoor.domain.Enums.ExecutionStatus;

@Service
public class DeviceMessageService {
    private static final Logger log = LoggerFactory.getLogger(DeviceMessageService.class);
    private final ObjectMapper objectMapper;
    private final DeviceRepository devices;
    private final AccessEventRepository accessEvents;
    private final SseService sse;

    public DeviceMessageService(ObjectMapper objectMapper, DeviceRepository devices,
                                AccessEventRepository accessEvents, SseService sse) {
        this.objectMapper = objectMapper;
        this.devices = devices;
        this.accessEvents = accessEvents;
        this.sse = sse;
    }

    @Transactional
    public void handle(String topic, byte[] bytes) {
        try {
            JsonNode payload = objectMapper.readTree(new String(bytes, StandardCharsets.UTF_8));
            String deviceId = payload.path("deviceId").asText();
            if (deviceId.isBlank()) return;
            Device device = devices.findByPublicId(deviceId).orElse(null);
            if (device == null) return;
            DeviceStatus status = parseStatus(payload.path("state").asText(payload.path("status").asText("LOCKED")));
            device.heartbeat(status);
            sse.broadcast("device-status", Map.of(
                    "deviceId", device.getPublicId(), "doorId", device.getDoor().getPublicId(),
                    "status", status.name(), "at", Instant.now().toString()));
            String requestId = payload.path("requestId").asText();
            if (!requestId.isBlank()) {
                accessEvents.findByRequestId(requestId).ifPresent(event -> {
                    if (status == DeviceStatus.UNLOCKED) event.setExecutionStatus(ExecutionStatus.UNLOCKED);
                    else if (status == DeviceStatus.ERROR) event.setExecutionStatus(ExecutionStatus.DEVICE_ERROR);
                });
            }
        } catch (Exception exception) {
            log.warn("Invalid device message on {}: {}", topic, exception.getMessage());
        }
    }

    private DeviceStatus parseStatus(String value) {
        try { return DeviceStatus.valueOf(value); }
        catch (Exception exception) { return DeviceStatus.ERROR; }
    }
}
