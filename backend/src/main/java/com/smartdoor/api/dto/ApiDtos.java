package com.smartdoor.api.dto;

import com.smartdoor.domain.Enums.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.time.*;
import java.util.List;

public final class ApiDtos {
    private ApiDtos() {}

    public record LoginRequest(@NotBlank String username, @NotBlank String password) {}
    public record SessionResponse(boolean authenticated, String username) {}

    public record UserCreateRequest(
            @NotBlank @Size(max = 160) String fullName,
            @Email @Size(max = 190) String email,
            @Size(max = 50) String phone,
            @NotBlank @Size(max = 30) String role) {}

    public record UserUpdateRequest(
            @NotBlank @Size(max = 160) String fullName,
            @Email @Size(max = 190) String email,
            @Size(max = 50) String phone,
            @NotBlank @Size(max = 30) String role) {}

    public record UserResponse(String id, String publicId, int code, String fullName, String email, String phone,
                               String role, UserStatus status, Instant createdAt, Instant updatedAt) {}
    public record NextUserCodeResponse(int code) {}
    public record StatusRequest(@NotNull UserStatus status) {}

    public record RoleRequest(
            @NotBlank @Size(max = 30) String code,
            @NotBlank @Size(max = 80) String name,
            @NotNull UserRole modelRole) {}
    public record RoleUpdateRequest(
            @NotBlank @Size(max = 80) String name,
            @NotNull UserRole modelRole) {}
    public record RoleResponse(String code, String name, UserRole modelRole,
                               Instant createdAt, Instant updatedAt) {}

    public record DoorResponse(String id, String publicId, String name, DoorStatus status) {}
    public record PermissionRequest(@NotNull List<@NotBlank String> doorIds) {}
    public record PermissionResponse(String doorId, String doorPublicId, String doorName) {}

    public record ScheduleItem(@NotNull DayOfWeek dayOfWeek, @NotNull LocalTime startTime, @NotNull LocalTime endTime) {}
    public record ScheduleRequest(@NotNull List<@Valid ScheduleItem> schedules) {}
    public record ScheduleResponse(String id, DayOfWeek dayOfWeek, LocalTime startTime, LocalTime endTime) {}

    public record CredentialRequest(@NotNull @Future Instant expiresAt, @NotNull UsageMode usageMode,
                                    @Positive Integer maxUses) {}
    public record CredentialResponse(String id, String userId, CredentialStatus status, UsageMode usageMode,
                                     int usageCount, Integer maxUses, Instant issuedAt, Instant expiresAt,
                                     String token, String qrDataUrl) {}

    public record AccessVerifyRequest(@NotBlank String doorId, @NotBlank String deviceId, @NotBlank String qrToken) {}
    public record AccessDecisionResponse(String requestId, String decision, String resultCode,
                                         ExecutionStatus executionStatus, List<String> path,
                                         String userPublicId, String userName) {}

    public record AccessEventResponse(String id, String requestId, String userPublicId, String userName,
                                      String doorPublicId, String devicePublicId, String resultCode,
                                      ExecutionStatus executionStatus, String modelResult,
                                      String modelVersion, List<String> evaluationPath, Instant occurredAt) {}

    public record DeviceResponse(String id, String publicId, String doorPublicId, DeviceType type,
                                 DeviceStatus status, Instant lastHeartbeatAt, boolean online) {}

    public record ModelInfoResponse(String version, double accuracy, double precision, double recall,
                                    String confusionMatrix, String tree, int trainingRows) {}

    public record ApiError(String code, String message, Instant timestamp) {}
}
