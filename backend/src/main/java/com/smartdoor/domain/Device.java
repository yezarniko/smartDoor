package com.smartdoor.domain;

import jakarta.persistence.*;

import java.time.Instant;

import static com.smartdoor.domain.Enums.DeviceStatus;
import static com.smartdoor.domain.Enums.DeviceType;

@Entity
@Table(name = "devices")
public class Device extends BaseEntity {
    @Column(name = "public_id", nullable = false, unique = true, length = 40)
    private String publicId;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "door_id", nullable = false)
    private Door door;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private DeviceType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private DeviceStatus status;

    @Column(name = "last_heartbeat_at")
    private Instant lastHeartbeatAt;

    protected Device() {}

    public Device(String publicId, Door door, DeviceType type) {
        this.publicId = publicId;
        this.door = door;
        this.type = type;
        this.status = DeviceStatus.OFFLINE;
    }

    public void heartbeat(DeviceStatus status) { this.status = status; this.lastHeartbeatAt = Instant.now(); }
    public String getPublicId() { return publicId; }
    public Door getDoor() { return door; }
    public DeviceType getType() { return type; }
    public DeviceStatus getStatus() { return status; }
    public Instant getLastHeartbeatAt() { return lastHeartbeatAt; }
}

