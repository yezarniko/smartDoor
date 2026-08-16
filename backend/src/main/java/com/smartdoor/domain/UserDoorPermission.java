package com.smartdoor.domain;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_door_permissions", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "door_id"}))
public class UserDoorPermission {
    @Id
    @Column(length = 36)
    private String id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserAccount user;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "door_id", nullable = false)
    private Door door;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected UserDoorPermission() {}

    public UserDoorPermission(UserAccount user, Door door) {
        this.id = UUID.randomUUID().toString();
        this.user = user;
        this.door = door;
        this.createdAt = Instant.now();
    }

    public String getId() { return id; }
    public UserAccount getUser() { return user; }
    public Door getDoor() { return door; }
}

