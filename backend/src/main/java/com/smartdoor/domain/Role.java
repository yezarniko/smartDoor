package com.smartdoor.domain;

import jakarta.persistence.*;

import java.time.Instant;

import static com.smartdoor.domain.Enums.UserRole;

@Entity
@Table(name = "roles")
public class Role {
    @Id
    @Column(length = 30)
    private String code;

    @Column(nullable = false, unique = true, length = 80)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "model_role", nullable = false, length = 30)
    private UserRole modelRole;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Role() {}

    public Role(String code, String name, UserRole modelRole) {
        this.code = normalizeCode(code);
        this.createdAt = Instant.now();
        update(name, modelRole);
    }

    public void update(String name, UserRole modelRole) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Role name is required");
        this.name = name.trim();
        this.modelRole = modelRole;
        this.updatedAt = Instant.now();
    }

    public static String normalizeCode(String value) {
        String normalized = value == null ? "" : value.trim().toUpperCase();
        if (!normalized.matches("[A-Z][A-Z0-9_]{0,29}")) {
            throw new IllegalArgumentException("Role code must start with a letter and use only letters, numbers, or underscores");
        }
        return normalized;
    }

    public String getCode() { return code; }
    public String getName() { return name; }
    public UserRole getModelRole() { return modelRole; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
