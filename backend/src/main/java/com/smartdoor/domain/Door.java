package com.smartdoor.domain;

import jakarta.persistence.*;

import static com.smartdoor.domain.Enums.DoorStatus;

@Entity
@Table(name = "doors")
public class Door extends BaseEntity {
    @Column(name = "public_id", nullable = false, unique = true, length = 40)
    private String publicId;

    @Column(nullable = false, length = 120)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private DoorStatus status;

    protected Door() {}

    public Door(String publicId, String name) {
        this.publicId = publicId;
        this.name = name;
        this.status = DoorStatus.ACTIVE;
    }

    public String getPublicId() { return publicId; }
    public String getName() { return name; }
    public DoorStatus getStatus() { return status; }
}

