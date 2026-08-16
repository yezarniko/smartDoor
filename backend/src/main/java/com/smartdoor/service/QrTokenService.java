package com.smartdoor.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.qrcode.QRCodeWriter;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;

@Service
public class QrTokenService {
    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder DECODER = Base64.getUrlDecoder();
    private final ObjectMapper objectMapper;

    @Value("${smartdoor.qr.secret}") private String secret;

    public QrTokenService(ObjectMapper objectMapper) { this.objectMapper = objectMapper; }

    @PostConstruct
    void validateSecret() {
        if (secret == null || secret.length() < 32) {
            throw new IllegalStateException("SMARTDOOR_QR_SECRET must contain at least 32 characters");
        }
    }

    public String create(String credentialId, String nonce, Instant issuedAt, Instant expiresAt) {
        try {
            String header = encode(objectMapper.writeValueAsBytes(new Header("HS256", "SDQR", 1)));
            String payload = encode(objectMapper.writeValueAsBytes(
                    new TokenPayload(credentialId, nonce, issuedAt.getEpochSecond(), expiresAt.getEpochSecond())));
            String content = header + "." + payload;
            return content + "." + encode(sign(content));
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to create QR credential", exception);
        }
    }

    public TokenPayload verify(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) throw new IllegalArgumentException("TOKEN_TAMPERED");
            byte[] expected = sign(parts[0] + "." + parts[1]);
            byte[] supplied = DECODER.decode(parts[2]);
            if (!MessageDigest.isEqual(expected, supplied)) throw new IllegalArgumentException("TOKEN_TAMPERED");
            Header header = objectMapper.readValue(DECODER.decode(parts[0]), Header.class);
            if (!"HS256".equals(header.alg()) || !"SDQR".equals(header.typ()) || header.v() != 1) {
                throw new IllegalArgumentException("TOKEN_TAMPERED");
            }
            return objectMapper.readValue(DECODER.decode(parts[1]), TokenPayload.class);
        } catch (IllegalArgumentException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalArgumentException("TOKEN_TAMPERED");
        }
    }

    public String qrDataUrl(String token) {
        try {
            var matrix = new QRCodeWriter().encode(token, BarcodeFormat.QR_CODE, 420, 420);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", output);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(output.toByteArray());
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to render QR image", exception);
        }
    }

    public String nonceHash(String nonce) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(nonce.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(hash);
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private byte[] sign(String content) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return mac.doFinal(content.getBytes(StandardCharsets.UTF_8));
    }

    private static String encode(byte[] bytes) { return ENCODER.encodeToString(bytes); }

    public record Header(String alg, String typ, int v) {}
    public record TokenPayload(String cid, String nonce, long iat, long exp) {}
}

