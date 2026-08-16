package com.smartdoor.repository;

import com.smartdoor.domain.UserDoorPermission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PermissionRepository extends JpaRepository<UserDoorPermission, String> {
    List<UserDoorPermission> findAllByUserId(String userId);
    boolean existsByUserIdAndDoorId(String userId, String doorId);
    void deleteAllByUserId(String userId);
}

