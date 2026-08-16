package com.smartdoor.api;

import com.smartdoor.api.dto.ApiDtos.DeviceResponse;
import com.smartdoor.repository.DeviceRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/devices")
public class DeviceController {
    private final DeviceRepository devices;
    public DeviceController(DeviceRepository devices) { this.devices = devices; }

    @GetMapping
    @Transactional(readOnly = true)
    public List<DeviceResponse> list() {
        Instant threshold = Instant.now().minusSeconds(20);
        return devices.findAll().stream().map(device -> new DeviceResponse(
                device.getId(), device.getPublicId(), device.getDoor().getPublicId(), device.getType(),
                device.getStatus(), device.getLastHeartbeatAt(),
                device.getLastHeartbeatAt() != null && device.getLastHeartbeatAt().isAfter(threshold))).toList();
    }
}
