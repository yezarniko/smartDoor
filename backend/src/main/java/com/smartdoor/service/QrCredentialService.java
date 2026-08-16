package com.smartdoor.service;

import com.smartdoor.api.ResourceNotFoundException;
import com.smartdoor.api.dto.ApiDtos.CredentialRequest;
import com.smartdoor.api.dto.ApiDtos.CredentialResponse;
import com.smartdoor.domain.Enums.CredentialStatus;
import com.smartdoor.domain.QrCredential;
import com.smartdoor.domain.UserAccount;
import com.smartdoor.repository.QrCredentialRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.List;

@Service
public class QrCredentialService {
    private final QrCredentialRepository credentials;
    private final UserService users;
    private final QrTokenService tokens;
    private final SecureRandom random = new SecureRandom();

    public QrCredentialService(QrCredentialRepository credentials, UserService users, QrTokenService tokens) {
        this.credentials = credentials;
        this.users = users;
        this.tokens = tokens;
    }

    @Transactional
    public CredentialResponse create(String userId, CredentialRequest request) {
        if (!request.expiresAt().isAfter(Instant.now())) throw new IllegalArgumentException("Expiry must be in the future");
        UserAccount user = users.requireUser(userId);
        byte[] nonceBytes = new byte[32];
        random.nextBytes(nonceBytes);
        String nonce = Base64.getUrlEncoder().withoutPadding().encodeToString(nonceBytes);
        QrCredential credential = credentials.saveAndFlush(new QrCredential(user, tokens.nonceHash(nonce),
                request.usageMode(), request.maxUses(), request.expiresAt()));
        String token = tokens.create(credential.getId(), nonce, credential.getIssuedAt(), credential.getExpiresAt());
        return toResponse(credential, token, tokens.qrDataUrl(token));
    }

    @Transactional(readOnly = true)
    public List<CredentialResponse> list(String userId) {
        users.requireUser(userId);
        return credentials.findAllByUserIdOrderByIssuedAtDesc(userId).stream()
                .map(credential -> toResponse(credential, null, null)).toList();
    }

    @Transactional
    public CredentialResponse revoke(String credentialId) {
        QrCredential credential = require(credentialId);
        credential.revoke();
        return toResponse(credential, null, null);
    }

    @Transactional
    public CredentialResponse regenerate(String credentialId, CredentialRequest request) {
        QrCredential old = require(credentialId);
        old.revoke();
        return create(old.getUser().getId(), request);
    }

    public QrCredential require(String id) {
        return credentials.findById(id).orElseThrow(() -> new ResourceNotFoundException("QR credential not found"));
    }

    private static CredentialResponse toResponse(QrCredential credential, String token, String dataUrl) {
        CredentialStatus displayStatus = credential.getStatus();
        if (displayStatus == CredentialStatus.ACTIVE && credential.getExpiresAt().isBefore(Instant.now())) {
            displayStatus = CredentialStatus.EXPIRED;
        }
        return new CredentialResponse(credential.getId(), credential.getUser().getId(), displayStatus,
                credential.getUsageMode(), credential.getUsageCount(), credential.getMaxUses(),
                credential.getIssuedAt(), credential.getExpiresAt(), token, dataUrl);
    }
}

