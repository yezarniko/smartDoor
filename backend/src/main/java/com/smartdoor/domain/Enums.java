package com.smartdoor.domain;

public final class Enums {
    private Enums() {}

    public enum UserRole { ADMIN, STAFF, VISITOR }
    public enum UserStatus { ACTIVE, INACTIVE }
    public enum DoorStatus { ACTIVE, INACTIVE }
    public enum CredentialStatus { ACTIVE, REVOKED, EXPIRED }
    public enum UsageMode { ONE_TIME, MULTI_USE }
    public enum DeviceType { TERMINAL, ACTUATOR, SIMULATOR }
    public enum DeviceStatus { OFFLINE, LOCKED, UNLOCKING, UNLOCKED, RELOCKING, ERROR }
    public enum ExecutionStatus { DENIED, GRANTED_COMMAND_SENT, UNLOCKED, DEVICE_TIMEOUT, DEVICE_ERROR }
}

