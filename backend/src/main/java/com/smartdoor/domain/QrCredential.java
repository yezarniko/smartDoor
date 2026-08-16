package com.smartdoor.domain;

import jakarta.persistence.*;

import java.time.Instant;

import static com.smartdoor.domain.Enums.CredentialStatus;
import static com.smartdoor.domain.Enums.UsageMode;

@Entity
@Table(name = "qr_credentials")
public class QrCredential extends BaseEntity {
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserAccount user;

    @Column(name = "nonce_hash", nullable = false, length = 64)
    private String nonceHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CredentialStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "usage_mode", nullable = false, length = 30)
    private UsageMode usageMode;

    @Column(name = "usage_count", nullable = false)
    private int usageCount;

    @Column(name = "max_uses")
    private Integer maxUses;

    @Column(name = "issued_at", nullable = false)
    private Instant issuedAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    protected QrCredential() {}

    public QrCredential(UserAccount user, String nonceHash, UsageMode usageMode, Integer maxUses, Instant expiresAt) {
        this.user = user;
        this.nonceHash = nonceHash;
        this.usageMode = usageMode;
        this.maxUses = usageMode == UsageMode.ONE_TIME ? 1 : maxUses;
        this.status = CredentialStatus.ACTIVE;
        this.issuedAt = Instant.now();
        this.expiresAt = expiresAt;
    }

    public boolean isExhausted() { return maxUses != null && usageCount >= maxUses; }
    public void useOnce() { usageCount++; }
    public void revoke() { status = CredentialStatus.REVOKED; revokedAt = Instant.now(); }
    public void expire() { status = CredentialStatus.EXPIRED; }

    public UserAccount getUser() { return user; }
    public String getNonceHash() { return nonceHash; }
    public CredentialStatus getStatus() { return status; }
    public UsageMode getUsageMode() { return usageMode; }
    public int getUsageCount() { return usageCount; }
    public Integer getMaxUses() { return maxUses; }
    public Instant getIssuedAt() { return issuedAt; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getRevokedAt() { return revokedAt; }
}

