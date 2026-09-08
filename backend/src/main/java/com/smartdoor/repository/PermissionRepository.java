package com.smartdoor.repository;

import com.smartdoor.domain.UserDoorPermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface PermissionRepository extends JpaRepository<UserDoorPermission, String> {
    List<UserDoorPermission> findAllByUserId(String userId);
    boolean existsByUserIdAndDoorId(String userId, String doorId);
    @Modifying
    @Query("delete from UserDoorPermission p where p.user.id = :userId")
    void deleteAllByUserId(String userId);
}
