package com.smartdoor.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

import java.time.Instant;
import java.util.UUID;

@MappedSuperclass
public abstract class BaseEntity {
    @Id
    @Column(length = 36, nullable = false, updatable = false)
    protected String id;

    @Column(nullable = false, updatable = false)
    protected Instant createdAt;

    @Column(nullable = false)
    protected Instant updatedAt;

    @PrePersist
    protected void prePersist() {
        if (id == null) id = UUID.randomUUID().toString();
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void preUpdate() {
        updatedAt = Instant.now();
    }

    public String getId() { return id; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}

