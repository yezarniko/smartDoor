package com.smartdoor.domain;

import jakarta.persistence.*;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "access_schedules")
public class AccessSchedule {
    @Id
    @Column(length = 36)
    private String id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserAccount user;

    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false, length = 12)
    private DayOfWeek dayOfWeek;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected AccessSchedule() {}

    public AccessSchedule(UserAccount user, DayOfWeek dayOfWeek, LocalTime startTime, LocalTime endTime) {
        if (!startTime.isBefore(endTime)) throw new IllegalArgumentException("Start time must be before end time");
        this.id = UUID.randomUUID().toString();
        this.user = user;
        this.dayOfWeek = dayOfWeek;
        this.startTime = startTime;
        this.endTime = endTime;
        this.createdAt = Instant.now();
    }

    public String getId() { return id; }
    public DayOfWeek getDayOfWeek() { return dayOfWeek; }
    public LocalTime getStartTime() { return startTime; }
    public LocalTime getEndTime() { return endTime; }
}

