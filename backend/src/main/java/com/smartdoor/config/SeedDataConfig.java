package com.smartdoor.config;

import com.smartdoor.domain.*;
import com.smartdoor.repository.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import static com.smartdoor.domain.Enums.DeviceType;

@Component
public class SeedDataConfig implements ApplicationRunner {
    private final AdministratorRepository administrators;
    private final DoorRepository doors;
    private final DeviceRepository devices;
    private final PasswordEncoder encoder;

    @Value("${smartdoor.admin.username}") private String adminUsername;
    @Value("${smartdoor.admin.password}") private String adminPassword;

    public SeedDataConfig(AdministratorRepository administrators, DoorRepository doors,
                          DeviceRepository devices, PasswordEncoder encoder) {
        this.administrators = administrators;
        this.doors = doors;
        this.devices = devices;
        this.encoder = encoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        administrators.findByUsername(adminUsername)
                .orElseGet(() -> administrators.save(new Administrator(adminUsername, encoder.encode(adminPassword))));

        Door door = doors.findByPublicId("DOOR-01")
                .orElseGet(() -> doors.save(new Door("DOOR-01", "Main Door")));
        devices.findByPublicId("TERMINAL-01")
                .orElseGet(() -> devices.save(new Device("TERMINAL-01", door, DeviceType.TERMINAL)));
        devices.findByPublicId("SIM-DOOR-01")
                .orElseGet(() -> devices.save(new Device("SIM-DOOR-01", door, DeviceType.SIMULATOR)));
        devices.findByPublicId("ESP32-DOOR-01")
                .orElseGet(() -> devices.save(new Device("ESP32-DOOR-01", door, DeviceType.ACTUATOR)));
    }
}
