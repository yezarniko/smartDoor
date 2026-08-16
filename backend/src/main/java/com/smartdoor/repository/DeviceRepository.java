package com.smartdoor.repository;

import com.smartdoor.domain.Device;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DeviceRepository extends JpaRepository<Device, String> {
    Optional<Device> findByPublicId(String publicId);
    List<Device> findAllByDoorId(String doorId);
}

