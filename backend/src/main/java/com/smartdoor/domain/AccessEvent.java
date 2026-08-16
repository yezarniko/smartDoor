package com.smartdoor.domain;

import jakarta.persistence.*;

import java.time.Instant;

import static com.smartdoor.domain.Enums.ExecutionStatus;

@Entity
@Table(name = "access_events")
public class AccessEvent extends BaseEntity {
    @Column(name = "request_id", nullable = false, unique = true, length = 36)
    private String requestId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private UserAccount user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "credential_id")
    private QrCredential credential;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "door_id")
    private Door door;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id")
    private Device device;

    @Column(name = "result_code", nullable = false, length = 50)
    private String resultCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "execution_status", nullable = false, length = 40)
    private ExecutionStatus executionStatus;

    @Column(name = "model_result", length = 30)
    private String modelResult;

    @Column(name = "model_version", length = 80)
    private String modelVersion;

    @Column(name = "evaluation_path", nullable = false, columnDefinition = "TEXT")
    private String evaluationPath;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    protected AccessEvent() {}

    public AccessEvent(String requestId, UserAccount user, QrCredential credential, Door door, Device device,
                       String resultCode, ExecutionStatus executionStatus, String modelResult,
                       String modelVersion, String evaluationPath) {
        this.requestId = requestId;
        this.user = user;
        this.credential = credential;
        this.door = door;
        this.device = device;
        this.resultCode = resultCode;
        this.executionStatus = executionStatus;
        this.modelResult = modelResult;
        this.modelVersion = modelVersion;
        this.evaluationPath = evaluationPath;
        this.occurredAt = Instant.now();
    }

    public void setExecutionStatus(ExecutionStatus status) { this.executionStatus = status; }
    public String getRequestId() { return requestId; }
    public UserAccount getUser() { return user; }
    public Door getDoor() { return door; }
    public Device getDevice() { return device; }
    public String getResultCode() { return resultCode; }
    public ExecutionStatus getExecutionStatus() { return executionStatus; }
    public String getModelResult() { return modelResult; }
    public String getModelVersion() { return modelVersion; }
    public String getEvaluationPath() { return evaluationPath; }
    public Instant getOccurredAt() { return occurredAt; }
}
