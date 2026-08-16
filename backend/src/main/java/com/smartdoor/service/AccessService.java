package com.smartdoor.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartdoor.api.dto.ApiDtos.AccessDecisionResponse;
import com.smartdoor.api.dto.ApiDtos.AccessEventResponse;
import com.smartdoor.api.dto.ApiDtos.AccessVerifyRequest;
import com.smartdoor.domain.*;
import com.smartdoor.repository.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.*;
import java.util.*;

import static com.smartdoor.domain.Enums.*;

@Service
public class AccessService {
    private final DoorRepository doors;
    private final DeviceRepository devices;
    private final QrCredentialRepository credentials;
    private final PermissionRepository permissions;
    private final ScheduleRepository schedules;
    private final AccessEventRepository events;
    private final QrTokenService tokens;
    private final DecisionTreeService decisionTree;
    private final MqttDoorService mqtt;
    private final ObjectMapper objectMapper;

    @Value("${smartdoor.time-zone}") private String timeZone;

    public AccessService(DoorRepository doors, DeviceRepository devices, QrCredentialRepository credentials,
                         PermissionRepository permissions, ScheduleRepository schedules, AccessEventRepository events,
                         QrTokenService tokens, DecisionTreeService decisionTree, MqttDoorService mqtt,
                         ObjectMapper objectMapper) {
        this.doors = doors;
        this.devices = devices;
        this.credentials = credentials;
        this.permissions = permissions;
        this.schedules = schedules;
        this.events = events;
        this.tokens = tokens;
        this.decisionTree = decisionTree;
        this.mqtt = mqtt;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public AccessDecisionResponse verify(AccessVerifyRequest request) {
        String requestId = UUID.randomUUID().toString();
        List<String> path = new ArrayList<>();
        Door door = doors.findByPublicId(request.doorId()).orElse(null);
        Device terminal = devices.findByPublicId(request.deviceId()).orElse(null);
        if (door == null) return deny(requestId, null, null, null, terminal, "DOOR_NOT_PERMITTED", path);
        if (terminal == null || terminal.getType() != DeviceType.TERMINAL ||
                !terminal.getDoor().getId().equals(door.getId())) {
            return deny(requestId, null, null, door, terminal, "DEVICE_UNREGISTERED", path);
        }
        path.add("TERMINAL_REGISTERED");

        QrTokenService.TokenPayload payload;
        try {
            payload = tokens.verify(request.qrToken());
            path.add("SIGNATURE_VALID");
        } catch (IllegalArgumentException exception) {
            return deny(requestId, null, null, door, terminal, "TOKEN_TAMPERED", path);
        }

        QrCredential credential = credentials.findLockedById(payload.cid()).orElse(null);
        if (credential == null) return deny(requestId, null, null, door, terminal, "TOKEN_TAMPERED", path);
        UserAccount user = credential.getUser();
        if (!MessageDigest.isEqual(credential.getNonceHash().getBytes(StandardCharsets.UTF_8),
                tokens.nonceHash(payload.nonce()).getBytes(StandardCharsets.UTF_8))) {
            return deny(requestId, user, credential, door, terminal, "TOKEN_TAMPERED", path);
        }
        if (credential.getStatus() == CredentialStatus.REVOKED) {
            return deny(requestId, user, credential, door, terminal, "CREDENTIAL_REVOKED", path);
        }
        path.add("CREDENTIAL_ACTIVE");
        Instant now = Instant.now();
        if (credential.getExpiresAt().isBefore(now) || payload.exp() < now.getEpochSecond()) {
            credential.expire();
            return deny(requestId, user, credential, door, terminal, "CREDENTIAL_EXPIRED", path);
        }
        path.add("CREDENTIAL_NOT_EXPIRED");
        if (credential.isExhausted()) return deny(requestId, user, credential, door, terminal, "USAGE_LIMIT_REACHED", path);
        path.add("USAGE_AVAILABLE");
        if (user.getStatus() != UserStatus.ACTIVE) return deny(requestId, user, credential, door, terminal, "USER_INACTIVE", path);
        path.add("USER_ACTIVE");

        boolean doorAllowed = permissions.existsByUserIdAndDoorId(user.getId(), door.getId());
        boolean scheduleAllowed = scheduleAllows(user, now);
        boolean deviceRegistered = true;
        long failures = events.countRecentFailures(user.getId(), now.minus(Duration.ofMinutes(15)));
        String failureBucket = failures >= 4 ? "HIGH" : failures >= 2 ? "MEDIUM" : "LOW";
        DecisionTreeService.Prediction prediction = decisionTree.predict(
                doorAllowed, scheduleAllowed, deviceRegistered, user.getRole().name(), failureBucket);
        path.addAll(prediction.path());

        if (!doorAllowed) return deny(requestId, user, credential, door, terminal, "DOOR_NOT_PERMITTED", path, prediction);
        if (!scheduleAllowed) return deny(requestId, user, credential, door, terminal, "OUTSIDE_ALLOWED_TIME", path, prediction);
        if (!"AUTHORIZED".equals(prediction.result())) {
            return deny(requestId, user, credential, door, terminal, "MODEL_REJECTED", path, prediction);
        }

        Device actuator = findOnlineActuator(door, now);
        if (actuator == null) return deny(requestId, user, credential, door, terminal, "DEVICE_OFFLINE", path, prediction);
        path.add("ACTUATOR_ONLINE");

        credential.useOnce();
        AccessEvent event = new AccessEvent(requestId, user, credential, door, actuator, "GRANTED",
                ExecutionStatus.GRANTED_COMMAND_SENT, prediction.result(), prediction.version(), json(path));
        events.save(event);
        if (!mqtt.publishUnlock(door.getPublicId(), requestId, 5000)) {
            event.setExecutionStatus(ExecutionStatus.DEVICE_TIMEOUT);
            return new AccessDecisionResponse(requestId, "DENIED", "DEVICE_OFFLINE",
                    ExecutionStatus.DEVICE_TIMEOUT, path, user.getPublicId(), user.getFullName());
        }
        return new AccessDecisionResponse(requestId, "GRANTED", "GRANTED",
                ExecutionStatus.GRANTED_COMMAND_SENT, path, user.getPublicId(), user.getFullName());
    }

    @Transactional(readOnly = true)
    public List<AccessEventResponse> listEvents() {
        return events.findTop200ByOrderByOccurredAtDesc().stream().map(event -> new AccessEventResponse(
                event.getId(), event.getRequestId(), event.getUser() == null ? null : event.getUser().getPublicId(),
                event.getUser() == null ? null : event.getUser().getFullName(),
                event.getDoor() == null ? null : event.getDoor().getPublicId(),
                event.getDevice() == null ? null : event.getDevice().getPublicId(),
                event.getResultCode(), event.getExecutionStatus(), event.getModelResult(), event.getModelVersion(),
                parsePath(event.getEvaluationPath()), event.getOccurredAt())).toList();
    }

    private boolean scheduleAllows(UserAccount user, Instant instant) {
        ZonedDateTime local = instant.atZone(ZoneId.of(timeZone));
        LocalTime current = local.toLocalTime();
        return schedules.findAllByUserIdAndDayOfWeek(user.getId(), local.getDayOfWeek()).stream()
                .anyMatch(schedule -> !current.isBefore(schedule.getStartTime()) && current.isBefore(schedule.getEndTime()));
    }

    private Device findOnlineActuator(Door door, Instant now) {
        return devices.findAllByDoorId(door.getId()).stream()
                .filter(device -> device.getType() == DeviceType.ACTUATOR || device.getType() == DeviceType.SIMULATOR)
                .filter(device -> device.getLastHeartbeatAt() != null && device.getLastHeartbeatAt().isAfter(now.minusSeconds(20)))
                .filter(device -> device.getStatus() != DeviceStatus.OFFLINE && device.getStatus() != DeviceStatus.ERROR)
                .findFirst().orElse(null);
    }

    private AccessDecisionResponse deny(String requestId, UserAccount user, QrCredential credential, Door door,
                                        Device device, String code, List<String> path) {
        return deny(requestId, user, credential, door, device, code, path, null);
    }

    private AccessDecisionResponse deny(String requestId, UserAccount user, QrCredential credential, Door door,
                                        Device device, String code, List<String> path,
                                        DecisionTreeService.Prediction prediction) {
        path.add(code);
        events.save(new AccessEvent(requestId, user, credential, door, device, code, ExecutionStatus.DENIED,
                prediction == null ? null : prediction.result(), prediction == null ? null : prediction.version(), json(path)));
        return new AccessDecisionResponse(requestId, "DENIED", code, ExecutionStatus.DENIED, List.copyOf(path),
                user == null ? null : user.getPublicId(), user == null ? null : user.getFullName());
    }

    private String json(List<String> path) {
        try { return objectMapper.writeValueAsString(path); }
        catch (Exception exception) { return "[]"; }
    }

    private List<String> parsePath(String json) {
        try { return objectMapper.readValue(json, new TypeReference<>() {}); }
        catch (Exception exception) { return List.of(); }
    }
}

