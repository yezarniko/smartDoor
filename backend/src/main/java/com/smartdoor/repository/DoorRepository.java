package com.smartdoor.repository;

import com.smartdoor.domain.Door;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DoorRepository extends JpaRepository<Door, String> {
    Optional<Door> findByPublicId(String publicId);
}

